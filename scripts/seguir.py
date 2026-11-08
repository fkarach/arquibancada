"""Aplica um pedido feito pelo botão "+ Seguir" do app (aberto como issue no GitHub).

O app abre uma issue com título "Arquibancada: ..." e corpo em linhas "chave: valor":
    acao: seguir | nivel | parar
    id: <slug>                (para nivel/parar; no seguir é gerado do nome)
    nome: FURIA
    tipo: time | atleta | campeonato | modalidade | tema
    esporte: esports
    modalidade: cs2           (opcional)
    nivel: meu_time | normal | silenciar
    detalhe: texto livre      (opcional)

Só pedidos do dono do repositório são aceitos (o workflow confere antes).
Grava em dados/seguindo.json e escreve a resposta em resposta.txt.
"""
import json
import os
import re
import sys
import unicodedata

NIVEIS = {"meu_time", "normal", "silenciar"}
TIPOS = {"time", "atleta", "campeonato", "modalidade", "tema"}


def slug(texto):
    t = unicodedata.normalize("NFKD", texto).encode("ascii", "ignore").decode().lower()
    return re.sub(r"[^a-z0-9]+", "-", t).strip("-")[:60] or "interesse"


def campos(corpo):
    out = {}
    for linha in (corpo or "").splitlines():
        if ":" in linha:
            k, v = linha.split(":", 1)
            k = k.strip().lower()
            if re.fullmatch(r"[a-z_]{1,20}", k):
                out[k] = v.strip()[:200]
    return out


def main():
    evento = json.load(open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8"))
    f = campos(evento["issue"].get("body"))
    acao = f.get("acao", "seguir")
    caminho = "dados/seguindo.json"
    dados = json.load(open(caminho, encoding="utf-8"))
    lista = dados.setdefault("seguindo", [])

    if acao == "seguir":
        nome = f.get("nome", "").strip()
        if not nome:
            sys.exit("Pedido sem nome.")
        novo = {
            "id": f.get("id") or slug(f"{nome}-{f.get('modalidade') or f.get('esporte', '')}"),
            "nome": nome,
            "tipo": f.get("tipo") if f.get("tipo") in TIPOS else "time",
            "esporte": slug(f.get("esporte", "outro")).replace("-", "_"),
            "nivel": f.get("nivel") if f.get("nivel") in NIVEIS else "meu_time",
        }
        if f.get("modalidade"):
            novo["modalidade"] = slug(f["modalidade"]).replace("-", "_")
        if f.get("detalhe"):
            novo["detalhe"] = f["detalhe"]
        existente = next((i for i in lista if i.get("id") == novo["id"]), None)
        if existente:
            existente.update(novo)
            resposta = f"Atualizado: {nome} ({novo['nivel']})."
        else:
            lista.append(novo)
            resposta = f"Seguindo: {nome} ({novo['nivel']}). Os eventos entram na próxima atualização da agenda."
    elif acao in ("nivel", "parar"):
        alvo = next((i for i in lista if i.get("id") == f.get("id")), None)
        if not alvo:
            sys.exit(f"Interesse não encontrado: {f.get('id')}")
        if acao == "parar":
            lista.remove(alvo)
            resposta = f"Deixou de seguir: {alvo['nome']}."
        else:
            nivel = f.get("nivel")
            if nivel not in NIVEIS:
                sys.exit(f"Nível inválido: {nivel}")
            alvo["nivel"] = nivel
            resposta = f"{alvo['nome']} agora é {nivel}."
    else:
        sys.exit(f"Ação desconhecida: {acao}")

    with open(caminho, "w", encoding="utf-8") as arq:
        json.dump(dados, arq, ensure_ascii=False, indent=2)
    with open("resposta.txt", "w", encoding="utf-8") as arq:
        arq.write(resposta)
    print(resposta)


if __name__ == "__main__":
    main()
