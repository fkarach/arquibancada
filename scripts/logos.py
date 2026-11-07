"""Resolve os escudos/logos dos times listados em dados/times.json usando o TheSportsDB.

Roda no GitHub Actions (que tem acesso livre à internet). Mantém os logos já encontrados
e só procura os times novos. Resultado em dados/logos.json.
"""
import json
import time
import urllib.error
import urllib.parse
import urllib.request

ESPORTE = {"futebol": "Soccer", "nba": "Basketball", "nfl": "American Football"}
CHAVE = "123"  # chave pública gratuita do TheSportsDB


def buscar(nome):
    """Busca com paciência: o plano gratuito bloqueia rajadas (HTTP 429)."""
    url = f"https://www.thesportsdb.com/api/v1/json/{CHAVE}/searchteams.php?t={urllib.parse.quote(nome)}"
    for tentativa in range(5):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "arquibancada-app"})
            with urllib.request.urlopen(req, timeout=20) as r:
                dados = json.load(r)
            return dados.get("teams") or []
        except urllib.error.HTTPError as erro:
            espera = 30 * (tentativa + 1) if erro.code == 429 else 5
            print(f"  HTTP {erro.code}; nova tentativa em {espera}s")
            time.sleep(espera)
        except Exception as erro:
            print(f"  falhou: {erro}")
            time.sleep(5)
    return None  # None = não deu para consultar (diferente de "não encontrado")


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
        resultado = buscar(info["busca"])
        url = escolher(resultado, info) if resultado is not None else None
        if url:
            logos[nome] = url
        else:
            sem_logo.append(nome)
        time.sleep(6)  # bem abaixo do limite do plano gratuito
    saida = {"versao": 1, "logos": dict(sorted(logos.items())), "sem_logo": sorted(sem_logo)}
    with open("dados/logos.json", "w", encoding="utf-8") as f:
        json.dump(saida, f, ensure_ascii=False, indent=2)
    print(f"{len(logos)} logos, {len(sem_logo)} sem logo: {sem_logo}")


if __name__ == "__main__":
    main()
