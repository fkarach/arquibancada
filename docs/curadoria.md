# Curadoria da agenda — regras

Este arquivo é a regra que a atualização automática segue para escrever `dados/agenda.json`. Quem atualiza lê este arquivo e `dados/seguindo.json` antes de começar.

## Janela e cadência

- Agenda de **30 dias** à frente, a partir do dia da atualização.
- **Atualização completa a cada 3 dias, às 06:00** (horário de Brasília). Não há checagem diária: uma agenda de 30 dias não muda o bastante para justificar o custo.
- Eventos que já terminaram saem do arquivo.

## Interesses (`dados/seguindo.json`)

Lista única e livre. Nada é fixo: o Felipe adiciona, muda o nível ou remove pelo botão "+ Seguir" do app (vira uma issue no GitHub que uma automação aplica). Cada interesse tem:

- `id` (slug estável), `nome`, `tipo` (`time`, `atleta`, `campeonato`, `modalidade`, `tema`), `esporte` (slug livre: `futebol`, `nba`, `esports`, `kings_league`, `volei`...), `modalidade` opcional (ex.: `cs2`, `league_of_legends`), `detalhe` opcional (texto que explica o interesse), `nivel` (`meu_time`, `normal`, `silenciar`).
- **Interesse novo que você nunca viu:** pesquise o que ele é a partir de `nome`, `esporte`, `modalidade` e `detalhe` antes de buscar eventos. Se for ambíguo (dois times com o mesmo nome), escolha o mais provável para um torcedor brasileiro e registre a escolha em `observacoes`.

## O que entra

1. **`meu_time`:** todo evento do interesse. Time: todos os jogos. Atleta: toda prova em que está inscrito. Campeonato: todos os jogos relevantes, todo o mata-mata. Tema: tudo o que o `detalhe` descreve (ex.: "Brasil em grandes eventos" = seleções e atletas brasileiros em Copas do Mundo, inclusive a feminina, Olimpíadas, Pan, Mundiais e amistosos das seleções principais).
2. **`silenciar`:** nunca entra, nem em mega-evento — a não ser que o evento também seja de um interesse `meu_time`.
3. **`normal` (modalidades e campeonatos):** entram os mega-eventos e os jogos que passam na régua abaixo.
4. **Mega-eventos, sempre:** finais (Copa do Brasil, Libertadores, Sul-Americana, Champions, Super Bowl, NBA Finals), todo GP de F1, cards numerados de UFC, eventos com brasileiro protagonista. Em eSports seguidos como modalidade: o Mundial a partir das quartas, playoffs e final do campeonato brasileiro, Majors a partir dos playoffs.
5. **Régua — demais jogos entram com 3 pontos ou mais:**
   - dá pra ver (TV aberta, YouTube ou um serviço de `servicos`): +2
   - os dois lados em boa fase (metade de cima da tabela/conferência): +2
   - vale algo (clássico, mata-mata, briga direta por título, vaga ou rebaixamento): +2
   - brasileiro em destaque: +1
   - narrativa forte (estreia, recorde, despedida): +1
   - jogo só em canal que a pessoa não tem precisa de 4 pontos.
6. **Pisos por semana em temporada** (modalidades `normal`): NBA 5, NFL 3, futebol brasileiro 6, futebol europeu 4. Se um esporte não bater o piso, baixe a régua em 1 ponto só para ele.

## Onde assistir

- Liste todas as opções no Brasil, grátis primeiro.
- `tipo`: `tv_aberta`, `youtube`, `streaming`, `tv_fechada`, `ppv`. Twitch e canais oficiais gratuitos contam como `youtube` (grátis).
- `servico`: identificador que o app cruza com `servicos` (ex.: `prime_video`, `paramount_plus`, `globoplay`, `espn_disney`, `hbo_max`, `league_pass`, `nfl_game_pass`, `netflix`).
- `status_canal`: `confirmado` (emissora, liga ou organizador divulgou para este evento), `previsto` (regra de pacote ou jogo anterior; diga a origem em `origem_canal`), `desconhecido`.
- Nunca invente canal. Na dúvida, `desconhecido`.

## Regras de pacote conhecidas (2026/27, conferir a cada temporada)

- **NFL:** NFL Game Pass (DAZN) tem todos os jogos. sportv: quinta e domingo à noite + 1 jogo de domingo à tarde. ESPN/Disney+: 2 jogos de domingo + segunda à noite. ge TV (YouTube, grátis): 1 jogo de domingo à tarde por semana, escolhido perto da data. Netflix: jogos especiais.
- **NBA:** League Pass tem todos. Prime Video e ESPN 2/Disney+ têm jogos escolhidos por mês.
- **UFC:** Paramount+ (eventos numerados e Fight Nights).
- **F1:** sportv e Globoplay todas; TV Globo ao vivo em parte das corridas (confirmar por GP).
- **Champions:** TNT/HBO Max todos; SBT 1 jogo por rodada (definido na semana).
- **Seleção Brasileira:** costuma passar na TV Globo e no ge TV; confirmar por jogo.
- **Mundial de LoL / CBLOL:** canais oficiais da Riot no YouTube e Twitch, grátis.
- **CS2 (BLAST, PGL, IEM):** canais oficiais dos organizadores na Twitch/YouTube, grátis; transmissões em português variam por torneio.
- **Kings League Brasil:** canais oficiais da Kings League (YouTube/Twitch), grátis.

## Formato

- Horários sempre em Brasília com offset (`2026-11-08T17:00:00-03:00`). Sem horário definido: `"horario_definido": false` e só a data em `inicio`.
- `id` estável entre atualizações (mesmo evento, mesmo id).
- **`interesses`:** lista com os `id` de `seguindo.json` a que o evento pertence (ex.: `["palmeiras", "futebol"]`, `["selecao-brasileira", "brasil-grandes-eventos"]`). Todo evento tem pelo menos um. O app usa isso para níveis, silenciar e lembretes.
- **`meu_time`:** `true` se algum interesse do evento é `meu_time`.
- **`categoria`:** obrigatória quando o esporte não é um destes: `futebol`, `nfl`, `nba`, `f1`, `mma`, `esqui`. Formato `{"chave": "cs2", "sigla": "CS2", "rotulo": "CS2"}` (sigla com até 4 letras). Para eSports, use a modalidade como chave (`cs2`, `league_of_legends` → sigla `LOL`, `valorant` → `VAL`). É o que vira a bolinha do topo do app.
- **Times e logos:** em jogos entre equipes, preencha `times` com os dois nomes curtos exatamente como aparecem no título (ex.: `["Vasco", "Palmeiras"]`). Todo nome usado em `times` precisa existir em `dados/times.json` com `busca` (nome completo em inglês/sem acento, como no TheSportsDB), `esporte` e, no futebol, `pais`. Para times que não existem em bancos abertos (Kings League, organizações pequenas de eSports, nomes genéricos), use `"buscar": false`: o app mostra o monograma. Ao adicionar um time novo em `times.json`, o GitHub busca o logo sozinho e grava em `dados/logos.json`. Não edite `logos.json` à mão.
- Horário mudou: preencha `inicio_anterior` e `situacao: "alterado"`.
- `por_que_vale`: 1 a 2 fatos verificáveis, nunca opinião.
- Registre as fontes em `fontes` (URLs) no topo do arquivo.
