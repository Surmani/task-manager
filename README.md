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

### 1. Iniciar o banco de dados

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
JWT_SECRET=seu_segredo_personalizado_com_pelo_menos_64_caracteres_para_segurança_hs512
```

Um modelo está disponível em `backend/.env.example`.

---

## Visão Geral da API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/register` | Registrar novo usuário |
| POST | `/api/auth/login` | Login e obter JWT token |
| GET | `/api/projects` | Listar projetos do usuário |
| POST | `/api/projects` | Criar projeto (somente ADMIN) |
| PUT | `/api/projects/{id}` | Atualizar projeto (somente ADMIN) |
| DELETE | `/api/projects/{id}` | Deletar projeto (somente ADMIN) |
| GET | `/api/projects/{id}/report` | Contadores de tarefas por status e prioridade |
| GET | `/api/projects/{id}/tasks` | Listar tarefas com filtros e paginação |
| POST | `/api/projects/{id}/tasks` | Criar tarefa |
| PATCH | `/api/projects/{id}/tasks/{taskId}` | Atualizar tarefa parcialmente |
| DELETE | `/api/projects/{id}/tasks/{taskId}` | Deletar tarefa |
| GET | `/api/projects/{id}/tasks/search` | Busca textual por título ou descrição |
| GET | `/api/projects/{id}/tasks/{taskId}/history` | Histórico de alterações da tarefa |
| GET | `/api/users` | Listar usuários (somente ADMIN) |
| GET | `/api/users/search` | Buscar usuários por nome ou e-mail (somente ADMIN) |

Documentação interativa completa disponível em `http://localhost:8080/swagger-ui.html`.

---

## Decisões Técnicas e Tradeoffs

### Autenticação Stateless com JWT
Optamos por JWT stateless em vez de sessões no servidor. Qualquer instância do servidor consegue validar tokens sem estado compartilhado, facilitando o escalonamento horizontal. O tradeoff é que tokens não podem ser invalidados antes do vencimento — aceitável para este escopo, mas um sistema de produção se beneficiaria de uma blocklist de tokens (ex: Redis).

### PostgreSQL em vez de H2
Utilizamos PostgreSQL via Docker em vez de H2 em memória, embora o H2 simplificasse a configuração. A decisão reflete condições reais: o comportamento do PostgreSQL com restrições, índices e peculiaridades do JPQL difere do H2 de formas que importam. O H2 é usado apenas no perfil de teste (`application-test.yml`).

### JPA Specification para Filtragem Dinâmica
A listagem de tarefas suporta até 6 filtros opcionais (status, prioridade, responsável, intervalo de datas) mais ordenação. Em vez de escrever uma query JPQL para cada combinação, o `JpaSpecificationExecutor` constrói a cláusula WHERE dinamicamente em Java. Isso é mais fácil de manter e testar do que queries com concatenação de strings.

### Zustand em vez de Redux (Frontend)
O Zustand foi escolhido para gerenciamento de estado pela sua simplicidade. Para a escala desta aplicação (estado de autenticação + lista de tarefas + sem estado derivado complexo), o Redux adicionaria boilerplate desnecessário. O store do Zustand também é mais fácil de ler e testar. O tradeoff é ter menos ferramentas (sem Redux DevTools), o que é aceitável aqui.

### Atualização Otimista da Interface no Drag and Drop
Quando uma tarefa é arrastada entre colunas, a interface atualiza imediatamente (atualização otimista) e a chamada à API acontece em paralelo. Se a chamada falhar (ex: limite de WIP atingido), o store busca novamente do servidor e exibe uma mensagem de erro toast. Isso proporciona responsividade sem sacrificar a consistência dos dados.

### Localização das Regras de Negócio
Todas as regras de negócio residem na camada de serviço, não no controller ou no repositório. Isso permite testá-las unitariamente de forma isolada com Mockito, sem precisar subir um servidor web ou banco de dados.

### Log de Auditoria (Histórico de Tarefas)
Cada alteração de campo em uma tarefa é registrada em `task_history` com o valor anterior, novo valor, campo alterado, quem alterou e quando. Isso atende ao requisito de audit log e oferece rastreabilidade completa. O tradeoff são gravações adicionais no banco a cada atualização — aceitável nesta escala.

### ProblemDetail (RFC 7807)
Utilizamos o `ProblemDetail` nativo do Spring Boot 3 para respostas de erro em vez de um DTO de erro customizado. Este é o formato padrão, não requer configuração extra e está de acordo com a RFC 7807. Avaliadores e consumidores da API recebem respostas de erro consistentes e estruturadas em todos os endpoints.

---

## Estratégia de Testes

| Camada | Ferramenta | Quantidade |
|--------|-----------|------------|
| Repositório | `@SpringBootTest` + H2 | 17 testes |
| Service | JUnit 5 + Mockito | 16 testes |
| Controller | `@SpringBootTest` + MockMvc | 26 testes |
| **Total** | | **59 testes** |

**Por que `@SpringBootTest` nos repositórios em vez de `@DataJpaTest`?**  
O `@DataJpaTest` é mais rápido mas usa H2 por padrão e carrega apenas o contexto JPA. O `@SpringBootTest` com o perfil de teste sobe o contexto completo e detecta problemas de integração que o `@DataJpaTest` poderia não capturar.

---

## O que Faria Diferente com Mais Tempo

- **Cache com Redis** na listagem de tarefas e relatórios, com invalidação por projeto a cada mutação de tarefa
- **WebSocket** para notificações em tempo real quando uma tarefa é atribuída ao usuário logado, em vez de depender de atualizações manuais
- **Busca textual com PostgreSQL `tsvector`** em vez de queries LIKE — indexada, sensível ao idioma e significativamente mais rápida em escala
- **Docker multi-stage build** para o backend, produzindo uma imagem de produção enxuta
- **Pipeline CI/CD com GitHub Actions** rodando testes e lint a cada pull request
- **Testes E2E com Playwright** cobrindo o fluxo crítico: login → criar projeto → criar tarefa → arrastar para concluído
- **Mecanismo de refresh token** para evitar forçar o usuário a fazer login novamente após o vencimento do JWT
- **Guardas de rota baseados em permissão** no frontend com um sistema mais granular além do binário ADMIN/MEMBER atual
