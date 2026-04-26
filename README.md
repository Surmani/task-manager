# Task Manager

Sistema de gerenciamento de tarefas para equipes de desenvolvimento, criado como desafio técnico.

## Tech Stack

**Backend:** Java 21, Spring Boot 3.4, Spring Security + JWT, PostgreSQL, JPA/Hibernate, SpringDoc OpenAPI  
**Frontend:** React 18, TypeScript, Vite, Tailwind CSS, Zustand, @dnd-kit, React Router v6

## Como rodar

### Pré-requisitos

- Java 21
- Maven 3.9+
- Node 22+
- Docker

### 1. Iniciar o banco de dados (database)

```bash
docker compose up -d
```

### 2. Rodar o backend

```bash
cd backend
mvn spring-boot:run
```

API disponível em: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Rodar o frontend

```bash
cd frontend
npm install
npm run dev
```

App disponível em: `http://localhost:5173`

### Variáveis de ambiente (opcional)

O segredo JWT possui um valor padrão seguro para desenvolvimento. Para sobrescrevê-lo, crie o arquivo `backend/.env`:

```env
JWT_SECRET=seu_segredo_personalizado_com_pelo_menos_64_caracteres_for_hs512_security
```

Um modelo disponível em `backend/.env.example`.

---

## API Overview

| Metodo | Endpoint | Descrição |
|--------|----------|-------------|
| POST | `/api/auth/register` | Registrar um novo usuário |
| POST | `/api/auth/login` | Login e obter o JWT token |
| GET | `/api/projects` | Listar os usuários dos projetos |
| POST | `/api/projects` | Criar um projeto (somente ADMIN) |
| PUT | `/api/projects/{id}` | Atualizar o projeto (somente ADMIN) |
| DELETE | `/api/projects/{id}` | Deletar o projeto (somente ADMIN) |
| GET | `/api/projects/{id}/report` | Contadores de tarefas por status e prioridade |
| GET | `/api/projects/{id}/tasks` | Listar tarefas com filtros e paginação |
| POST | `/api/projects/{id}/tasks` | Criar tarefa |
| PATCH | `/api/projects/{id}/tasks/{taskId}` | Atualizar tarefa |
| DELETE | `/api/projects/{id}/tasks/{taskId}` | Deletar tarefa |
| GET | `/api/projects/{id}/tasks/search` | Busca de texto completa por título ou descrição |
| GET | `/api/projects/{id}/tasks/{taskId}/history` | Registro de auditoria das tarefas |
| GET | `/api/users` | Listar usuários (somente ADMIN) |
| GET | `/api/users/search` | Buscar usuários pelo nome ou e-mail (somente ADMIN) |

Documentação iterativa completa disponível em `http://localhost:8080/swagger-ui.html`.

---

## Decisões Técnicas e Concessões

### Autenticação sem Estado com JWT
Optamos por JWT sem estado em vez de sessões do lado do servidor. Isso significa que qualquer instância de servidor pode validar tokens sem estado compartilhado, facilitando o escalonamento horizontal. A desvantagem é que os tokens não podem ser invalidados antes do vencimento — aceitável para este escopo, mas um sistema de produção se beneficiaria de uma lista de bloqueio de tokens (por exemplo, Redis).

### PostgreSQL em vez de H2
Utilizamos PostgreSQL via Docker em vez de H2 em memória, embora o H2 simplificasse a configuração. A decisão reflete condições reais: o comportamento do PostgreSQL com restrições, índices e peculiaridades do JPQL difere do H2 em aspectos importantes. O H2 é usado apenas para o perfil de teste (`application-test.yml`).

### Especificação JPA para Filtragem Dinâmica
A listagem de tarefas suporta até 6 filtros opcionais (status, prioridade, responsável, intervalo de datas), além da classificação. Em vez de escrever uma consulta JPQL para cada combinação, o `JpaSpecificationExecutor` constrói a cláusula WHERE dinamicamente em Java. Isso é mais fácil de manter e testar do que consultas concatenadas por strings.

### Zustand em vez de Redux (Frontend)
O Zustand foi escolhido para gerenciamento de estado devido à sua simplicidade. Para a escala desta aplicação (estado de autenticação + lista de tarefas + sem estado derivado complexo), o Redux adicionaria complexidade desnecessária. O armazenamento do Zustand também é mais fácil de ler e testar. A desvantagem é a menor quantidade de ferramentas (sem Redux DevTools), o que é aceitável neste caso.

### Atualizações Otimistas da Interface para Arrastar e Soltar
Quando uma tarefa é arrastada entre colunas, a interface é atualizada imediatamente (atualização otimista) e a chamada à API ocorre em paralelo. Se a chamada à API falhar (por exemplo, limite de WIP atingido), o armazenamento busca novamente no servidor e exibe uma mensagem de erro. Isso proporciona uma sensação de responsividade sem sacrificar a correção.

### Atualizações Otimistas da Interface para Arrastar e Soltar ### Localização das Regras de Negócio
Todas as regras de negócio residem na camada de serviço, não no controlador ou repositório. Isso permite que elas sejam testadas unitariamente de forma isolada com o Mockito, sem a necessidade de provisionar um servidor web ou banco de dados.

### Log de Auditoria (Histórico de Tarefas)
Cada alteração de campo em uma tarefa é registrada em `task_history` com o valor anterior, o novo valor, o campo alterado, quem o alterou e quando. Isso atende ao requisito de log de auditoria e oferece às equipes rastreabilidade completa. A desvantagem é a necessidade de gravações adicionais no banco de dados a cada atualização — aceitável nesta escala.

### ProblemDetail (RFC 7807)
Utilizamos o `ProblemDetail` nativo do Spring Boot 3 para respostas de erro em vez de um DTO de erro personalizado. Este é o formato padrão, não requer nenhuma configuração extra e está de acordo com o que a RFC 7807 prescreve. Avaliadores e consumidores da API recebem respostas de erro consistentes e estruturadas em todos os endpoints.

