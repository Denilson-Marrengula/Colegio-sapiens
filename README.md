# Sapiens — Frontend e API H2

Interface integrada no Spring Boot, baseada no protótipo UIUX enviado: identidade azul, login em duas colunas, menu lateral, painel, gestão de utilizadores e formulário de criação por etapas.

## Executar
Requisitos: JDK 21 e Maven.
1. Pare a versão anterior.
2. Faça uma cópia da pasta original. Extraia este ZIP numa pasta nova.
3. O ZIP inclui a cópia H2 enviada nesta conversa, com a conta administrativa existente. Se cadastrou dados depois desse envio, com a aplicação parada substitua a pasta data deste ZIP pela sua pasta data mais recente.
4. Execute:
   mvn clean test
   mvn spring-boot:run
5. Abra http://localhost:8080 e use o nome de utilizador e palavra-passe da sua conta.
A conta inicial é admin / Admin123! apenas se manteve a configuração e a conta original.

## Implementado
- Login, mostrar/ocultar palavra-passe, mensagens de erro e terminar sessão.
- Dashboard com contagens reais, de acordo com as permissões.
- Turmas, professores, encarregados e alunos: pesquisa, paginação, consulta, criação, edição e remoção conforme o perfil.
- Associação de encarregados através da edição do aluno; preserva as selecções actuais.
- Utilizadores: listagem, pesquisa, filtro por perfil, criação em quatro etapas, edição e activação/desactivação.
- ADMIN pode gerir todos os perfis. SECRETARIA só cria/edita PROFESSOR, ALUNO e ENCARREGADO. Só ADMIN altera estado. A própria conta não pode ser desactivada nem mudar o seu próprio perfil/username.
- E-mail e telefone opcionais nas contas, adicionados ao esquema H2 pelo Hibernate. Login usa username: a autenticação por e-mail/telefone do protótipo não foi implementada.
- Relatórios simples com contagens e exportação CSV dos cadastros autorizados.
- Meu perfil e interface adaptada a telemóvel.
- CSS e JavaScript locais; não é necessário instalar npm para executar.

## Telas futuras do protótipo
Integrações externas, SMS/e-mail/push, reuniões, convocatórias e acesso aos educandos ainda não têm API. As respectivas telas mostram explicitamente «Em preparação», sem dados simulados nem botões de envio.
A recuperação de palavra-passe orienta a contactar a administração; não envia e-mail.
As contas ALUNO e ENCARREGADO só acedem ao perfil nesta etapa.
Professores consultam turmas e professores; não são inventadas associações «minhas turmas».
O histórico de último acesso e de actividade não é registado nesta versão.

## Segurança e dados
Os assets e a página de login são públicos; a API mantém as permissões existentes.
HTTP Basic é mantido. Credenciais ficam apenas na memória da página e são apagadas ao sair; recarregar pede novo login.
A interface envia X-Requested-With para evitar a janela HTTP Basic nos erros dos pedidos AJAX.
Não guarda palavras-passe em localStorage/sessionStorage nem exporta hashes.
No ambiente publicado, servir por HTTPS.
As alterações de estado e perfil são verificadas pelo servidor, além dos controlos da interface.
H2 é guardado em data/colegio_sapiens.mv.db. Não foi feita mudança para MySQL.

## Verificação
O JavaScript passou na verificação sintáctica do Node.
FrontendAccessTest acrescenta verificações de acesso público aos assets, protecção da API, edição de utilizadores, hash omitido, preservação de palavra-passe, conta desactivada e restrições da secretaria.
Execute mvn clean test no seu computador para validar as classes novas: Maven/JDK 21 não estão disponíveis no ambiente de preparação.
