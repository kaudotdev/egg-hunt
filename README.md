# EggHunt - Plugin de Caça aos Ovos para Minecraft

Plugin para servidores Minecraft que adiciona um evento de caça aos ovos com fase de combate.

## Características

- Sistema de caça aos ovos com temporizador configurável
- Fase de combate após a coleta inicial
- Interface gráfica para configuração do evento
- Sistema de pontuação e ranking
- Comandos administrativos para gerenciar o evento

## Requisitos

- Servidor Paper 1.20.4 ou superior
- Java 17 ou superior

## Instalação

### Usando Maven

1. Clone o repositório:
```bash
git clone https://github.com/seu-usuario/eggHunt.git
```

2. Navegue até o diretório do projeto:
```bash
cd eggHunt
```

3. Compile o projeto com Maven:
```bash
mvn clean package
```

4. O arquivo JAR compilado estará disponível na pasta `target/`

5. Copie o arquivo JAR para a pasta `plugins` do seu servidor Minecraft

6. Reinicie o servidor

### Configuração Inicial

Configure as áreas do evento usando os comandos:
- `/setcorner1` - Define o primeiro canto da área de jogo
- `/setcorner2` - Define o segundo canto da área de jogo
- `/setwaitingroom` - Define a sala de espera

## Comandos

| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/starthunt` | Inicia o evento de caça aos ovos | egghunt.admin |
| `/endevent` | Encerra o evento atual | egghunt.admin |
| `/event` | Abre a GUI de configuração do evento | egghunt.admin |
| `/setcorner1` | Define o primeiro canto da área de jogo | egghunt.admin |
| `/setcorner2` | Define o segundo canto da área de jogo | egghunt.admin |
| `/setwaitingroom` | Define a sala de espera | egghunt.admin |

## Configuração via GUI

Use o comando `/event` para abrir a interface gráfica de configuração, onde você pode:

- Ajustar o tempo do evento
- Definir a quantidade de ovos
- Gerenciar a lista de jogadores excluídos
- Iniciar o evento com as configurações personalizadas

## Permissões

- `egghunt.admin` - Permite acesso a todos os comandos do plugin

## Contribuição

Contribuições são bem-vindas! Sinta-se à vontade para abrir issues ou enviar pull requests.

## Licença

Este projeto está licenciado sob a licença MIT.

---

Desenvolvido com ❤ por Kaudotdev - Versão 1.5