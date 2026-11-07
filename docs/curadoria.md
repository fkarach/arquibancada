# Curadoria da agenda — regras

Este arquivo é a regra que a atualização automática segue para escrever `dados/agenda.json`. Quem atualiza lê este arquivo e `dados/seguindo.json` antes de começar.

## Janela e cadência

- Agenda de **30 dias** à frente, a partir do dia da atualização.
- **Atualização completa a cada 3 dias, às 06:00** (horário de Brasília).
- **Checagem de véspera, todo dia às 06:00:** só para eventos de nível `meu_time` que acontecem nas próximas 36 horas; confere horário, situação e canal. Não adiciona eventos novos.
- Eventos que já terminaram saem do arquivo.

## O que entra

1. **Meu time:** todo evento de quem está com `nivel: "meu_time"` em `seguindo.json`. Atleta individual: toda prova em que ele está inscrito.
2. **Silenciar:** nunca entra, nem em mega-evento.
3. **Mega-eventos, sempre:** finais (Copa do Brasil, Libertadores, Sul-Americana, Champions, Super Bowl, NBA Finals), todo GP de F1, cards numerados de UFC, eventos com brasileiro protagonista.
4. **eSports, sempre (modalidades seguidas):** Mundial de LoL a partir das quartas; playoffs e final do CBLOL; RLCS Majors e Mundial a partir dos playoffs; finais regionais da América do Sul de Rocket League quando houver time brasileiro.
5. **Demais jogos entram com 3 pontos ou mais:**
   - dá pra ver (TV aberta, YouTube ou um serviço de `servicos`): +2
   - os dois lados em boa fase (metade de cima da tabela/conferência): +2
   - vale algo (clássico, mata-mata, briga direta por título, vaga ou rebaixamento): +2
   - brasileiro em destaque: +1
   - narrativa forte (estreia, recorde, despedida): +1
   - jogo só em canal que a pessoa não tem precisa de 4 pontos.
6. **Pisos por semana em temporada:** NBA 5, NFL 3, futebol brasileiro 6, futebol europeu 4. Se um esporte não bater o piso, baixe a régua em 1 ponto só para ele.

## Onde assistir

- Liste todas as opções no Brasil, grátis primeiro.
- `tipo`: `tv_aberta`, `youtube`, `streaming`, `tv_fechada`, `ppv`.
- `servico`: identificador que o app cruza com `servicos` (ex.: `prime_video`, `paramount_plus`, `globoplay`, `espn_disney`, `hbo_max`, `league_pass`, `nfl_game_pass`, `netflix`).
- `status_canal`: `confirmado` (emissora, liga ou organizador divulgou para este evento), `previsto` (regra de pacote ou jogo anterior; diga a origem em `origem_canal`), `desconhecido`.
- Nunca invente canal. Na dúvida, `desconhecido`.

## Regras de pacote conhecidas (2026/27, conferir a cada temporada)

- **NFL:** NFL Game Pass (DAZN) tem todos os jogos. sportv: quinta e domingo à noite + 1 jogo de domingo à tarde. ESPN/Disney+: 2 jogos de domingo + segunda à noite. ge TV (YouTube, grátis): 1 jogo de domingo à tarde por semana, escolhido perto da data. Netflix: jogos especiais.
- **NBA:** League Pass tem todos. Prime Video e ESPN 2/Disney+ têm jogos escolhidos por mês.
- **UFC:** Paramount+ (eventos numerados e Fight Nights).
- **F1:** sportv e Globoplay todas; TV Globo ao vivo em parte das corridas (confirmar por GP).
- **Champions:** TNT/HBO Max todos; SBT 1 jogo por rodada (definido na semana).
- **Mundial de LoL / CBLOL:** canais oficiais da Riot no YouTube e Twitch, grátis.

## Formato

- Horários sempre em Brasília com offset (`2026-11-08T17:00:00-03:00`). Sem horário definido: `"horario_definido": false` e só a data em `inicio`.
- `id` estável entre atualizações (mesmo evento, mesmo id).
- Horário mudou: preencha `inicio_anterior` e `situacao: "alterado"`.
- `por_que_vale`: 1 a 2 fatos verificáveis, nunca opinião.
- Registre as fontes em `fontes` (URLs) no topo do arquivo.
