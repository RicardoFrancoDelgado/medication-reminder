# 💊 Medication Reminder

Aplicação backend desenvolvida em **Java 21 com Spring Boot** para envio automatizado de lembretes de medicamentos via **WhatsApp**, utilizando a API da Twilio. O sistema envia mensagens nos horários programados, aguarda a confirmação do usuário e realiza reenvios automáticos caso não haja resposta.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Fluxo de Funcionamento](#fluxo-de-funcionamento)
- [Banco de Dados](#banco-de-dados)
- [Configuração e Variáveis de Ambiente](#configuração-e-variáveis-de-ambiente)
- [Como Executar](#como-executar)
- [Endpoints](#endpoints)

---

## Visão Geral

O **Medication Reminder** é um serviço que monitora os horários de medicamentos cadastrados no banco de dados e envia lembretes personalizados via WhatsApp. Após o envio, o sistema:

1. Aguarda a resposta do usuário (`sim` ou `não`).
2. Confirma ou nega a tomada do medicamento e registra no banco.
3. Caso não haja resposta em 30 minutos, reenvia o lembrete (até 2 tentativas).
4. Expira a sessão automaticamente após o número máximo de tentativas.

---

## Tecnologias

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem principal |
| Spring Boot | 3.5.11 | Framework principal |
| Spring Data JPA | — | Persistência de dados |
| Spring Web | — | API REST e Webhook |
| PostgreSQL | — | Banco de dados relacional |
| Flyway | — | Migrations do banco de dados |
| Twilio SDK | 10.1.0 | Envio de mensagens via WhatsApp |
| Lombok | — | Redução de boilerplate |
| spring-dotenv | 4.0.0 | Carregamento de variáveis `.env` |

---

## Arquitetura

O projeto segue os princípios da **Clean Architecture**, organizando o código em camadas bem definidas:

```
src/main/java/br/com/medicationreminder/
│
├── domain/                         # Regras de negócio e contratos
│   ├── model/                      # Entidades de domínio
│   │   ├── User.java
│   │   ├── Medication.java
│   │   ├── ReminderSession.java
│   │   └── ReminderStatus.java     # Enum: PENDING, CONFIRMED, DENIED, EXPIRED
│   └── repository/                 # Interfaces de repositório (portas de saída)
│       ├── UserRepository.java
│       ├── MedicationRepository.java
│       └── ReminderSessionRepository.java
│
├── application/                    # Casos de uso da aplicação
│   ├── gateway/
│   │   └── MessageGateway.java     # Interface de mensageria (porta de saída)
│   └── usecase/
│       ├── SendReminderUseCase.java
│       ├── CheckPendingRemindersUseCase.java
│       └── HandlerUserResponseUseCase.java
│
└── infra/                          # Implementações concretas (adapters)
    ├── messaging/
    │   ├── TwilioMessageGateway.java   # Implementação do MessageGateway via Twilio
    │   └── TwilioProperties.java
    ├── scheduler/
    │   └── ReminderScheduler.java      # Jobs agendados com @Scheduled
    └── webhook/
        └── WhatsappWebhookController.java  # Recebimento de respostas via webhook
```

---

## Fluxo de Funcionamento

### 1. Envio do Lembrete

A cada minuto, o `ReminderScheduler` dispara o `SendReminderUseCase`, que consulta medicamentos com horário agendado na janela de `±1 minuto` do momento atual. Para cada medicamento encontrado, o sistema:
- Verifica se já foi enviado um lembrete hoje para aquele horário (evita duplicatas).
- Cria uma `ReminderSession` com status `PENDING`.
- Envia a mensagem via WhatsApp: *"Olá, {nome}! Está na hora de tomar o {medicamento}. Você já tomou? Responda **sim** ou **não**."*

### 2. Verificação de Pendências

A cada 30 minutos, o `CheckPendingRemindersUseCase` busca sessões com status `PENDING` que foram enviadas há mais de 30 minutos. Para cada sessão:
- **Se `retryCount < 2`**: incrementa o contador, reenvia o lembrete numerado (ex: *"lembrete 1/2"*) e atualiza `sentAt`.
- **Se `retryCount >= 2`**: marca a sessão como `EXPIRED`.

### 3. Recebimento da Resposta

O `WhatsappWebhookController` recebe mensagens de entrada da Twilio via `POST /webhook`. Após validar a assinatura da requisição (`X-Twilio-Signature`), o controller aciona o `HandlerUserResponseUseCase`:

| Resposta do usuário | Ação |
|---|---|
| `sim` | Sessão marcada como `CONFIRMED`, mensagem de confirmação enviada |
| `não` / `nao` | Sessão marcada como `DENIED`, mensagem de incentivo enviada |
| outro texto | Mensagem de instrução reenviada sem alterar o status |

### Diagrama do Fluxo

```
[Scheduler - a cada 1min]
        │
        ▼
  SendReminderUseCase ──► Cria ReminderSession (PENDING) ──► Envia WhatsApp
        
[Scheduler - a cada 30min]
        │
        ▼
  CheckPendingRemindersUseCase
        │
        ├─ retryCount < 2 ──► Reenvia mensagem, incrementa contador
        └─ retryCount >= 2 ──► Marca sessão como EXPIRED

[POST /webhook] (Twilio)
        │
        ▼
  HandlerUserResponseUseCase
        │
        ├─ "sim"  ──► CONFIRMED + mensagem positiva
        ├─ "nao"  ──► DENIED + mensagem de incentivo
        └─ outro  ──► Solicita resposta válida
```

---

## Banco de Dados

As migrations são gerenciadas pelo **Flyway** e executadas automaticamente na inicialização da aplicação.

### Tabelas

#### `tb_users`
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `name` | VARCHAR(100) | Nome do usuário |
| `whatsapp_number` | VARCHAR(20) | Número WhatsApp (único) |
| `active` | BOOLEAN | Usuário ativo |

#### `tb_medications`
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `user_id` | UUID (FK) | Referência ao usuário |
| `name` | VARCHAR(100) | Nome do medicamento |
| `active` | BOOLEAN | Medicamento ativo |

#### `tb_medication_scheduled_times`
| Coluna | Tipo | Descrição |
|---|---|---|
| `medication_id` | UUID (FK) | Referência ao medicamento |
| `scheduled_time` | TIME | Horário agendado para o lembrete |

#### `tb_reminder_sessions`
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único |
| `medication_id` | UUID (FK) | Referência ao medicamento |
| `scheduled_time` | TIME | Horário agendado |
| `sent_at` | TIMESTAMP | Data/hora do envio |
| `responded_at` | TIMESTAMP | Data/hora da resposta |
| `status` | VARCHAR(20) | Status da sessão (`PENDING`, `CONFIRMED`, `DENIED`, `EXPIRED`) |
| `retry_count` | INTEGER | Número de tentativas realizadas |

### Dados iniciais (V5)

A migration `V5` popula automaticamente um usuário e seu medicamento, usando variáveis de ambiente como placeholders do Flyway. O medicamento é cadastrado com horários padrão de **08:00** e **20:00**.

---

## Configuração e Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto com as seguintes variáveis:

```env
# Banco de dados
DB_URL=jdbc:postgresql://localhost:5432/medication_reminder
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha

# Twilio
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_WHATSAPP_NUMBER=+14155238886

# Dados iniciais (usados nas migrations do Flyway)
USER_NAME=Nome do Usuário
USER_WHATSAPP=+5511999999999
MEDICATION_NAME=Nome do Medicamento
```

> As variáveis de banco e Twilio são lidas diretamente pelo Spring via `application.properties`. As variáveis `user_*` e `MEDICATION_NAME` são usadas exclusivamente na migration `V5` para seed inicial.

---

## Como Executar

### Pré-requisitos

- Java 21+
- Maven 3.8+
- PostgreSQL em execução
- Conta na [Twilio](https://www.twilio.com/) com o sandbox de WhatsApp configurado
- Um serviço de túnel (ex: [ngrok](https://ngrok.com/)) para expor o webhook localmente

### Passos

**1. Clone o repositório**
```bash
git clone https://github.com/seu-usuario/medication-reminder.git
cd medication-reminder
```

**2. Configure o arquivo `.env`** com as variáveis descritas acima.

**3. Execute a aplicação**
```bash
./mvnw spring-boot:run
```

O Flyway criará as tabelas e inserirá os dados iniciais automaticamente.

**4. Configure o webhook na Twilio**

Exponha sua aplicação localmente com ngrok:
```bash
ngrok http 8080
```

No painel da Twilio, configure o webhook do sandbox de WhatsApp com a URL:
```
https://<seu-id>.ngrok.io/webhook
```

---

## Endpoints

### `POST /webhook`

Recebe mensagens de entrada do WhatsApp via Twilio.

**Parâmetros (form-data):**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `From` | string | Número de origem no formato `whatsapp:+55...` |
| `Body` | string | Texto da mensagem recebida |

**Headers obrigatórios:**

| Header | Descrição |
|---|---|
| `X-Twilio-Signature` | Assinatura HMAC-SHA1 gerada pela Twilio para validação |

**Respostas:**

| Código | Descrição |
|---|---|
| `200 OK` | Mensagem processada com sucesso |
| `400 Bad Request` | Parâmetros `From` ou `Body` ausentes |
| `403 Forbidden` | Assinatura Twilio inválida |
