"""Resolve os escudos/logos dos times listados em dados/times.json usando o TheSportsDB.

Roda no GitHub Actions (que tem acesso livre à internet). Mantém os logos já encontrados
e só procura os times novos. Resultado em dados/logos.json.
"""
import json
import time
import urllib.parse
import urllib.request

ESPORTE = {"futebol": "Soccer", "nba": "Basketball", "nfl": "American Football"}
CHAVES = ["123", "3"]


def buscar(nome):
    for chave in CHAVES:
        url = f"https://www.thesportsdb.com/api/v1/json/{chave}/searchteams.php?t={urllib.parse.quote(nome)}"
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "arquibancada-app"})
            with urllib.request.urlopen(req, timeout=20) as r:
                dados = json.load(r)
            return dados.get("teams") or []
        except Exception as erro:  # tenta a próxima chave
            print(f"  falhou com chave {chave}: {erro}")
            time.sleep(3)
    return []


def escolher(times, info):
    esporte = ESPORTE.get(info["esporte"])
    candidatos = [t for t in times if not esporte or t.get("strSport") == esporte]
    if info.get("pais"):
        do_pais = [t for t in candidatos if (t.get("strCountry") or "").lower() == info["pais"].lower()]
        candidatos = do_pais or candidatos
    for t in candidatos:
        badge = t.get("strBadge") or t.get("strTeamBadge")
        if badge:
            return badge.rstrip("/") + "/small"
    return None


def main():
    registro = json.load(open("dados/times.json", encoding="utf-8"))["times"]
    try:
        atual = json.load(open("dados/logos.json", encoding="utf-8"))
    except FileNotFoundError:
        atual = {"versao": 1, "logos": {}, "sem_logo": []}
    logos = atual.get("logos", {})
    sem_logo = []
    for nome, info in registro.items():
        if nome in logos:
            continue
        print(f"buscando {nome} ({info['busca']})")
        url = escolher(buscar(info["busca"]), info)
        if url:
            logos[nome] = url
        else:
            sem_logo.append(nome)
        time.sleep(2.2)  # limite do plano gratuito: 30 pedidos por minuto
    saida = {"versao": 1, "logos": dict(sorted(logos.items())), "sem_logo": sorted(sem_logo)}
    with open("dados/logos.json", "w", encoding="utf-8") as f:
        json.dump(saida, f, ensure_ascii=False, indent=2)
    print(f"{len(logos)} logos, {len(sem_logo)} sem logo: {sem_logo}")


if __name__ == "__main__":
    main()
