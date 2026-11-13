"""Builds data/wikidata-footballers.json: the extra "Did you mean" player universe (Wikidata, CC0).

Step 1: every footballer (P106 = association football player) with >= MIN_LINKS sitelinks.
Step 2: in batches, English label, English alt labels and birth year.
Keep players born >= MIN_BORN, sort by sitelinks (fame), keep the top LIMIT.

Usage:  python -I data/fetch_wikidata_players.py   (from the repo root; ~2 min, polite sequential requests)
"""
import csv, io, json, sys, time, urllib.parse, urllib.request
from pathlib import Path

ENDPOINT = "https://query.wikidata.org/sparql"
UA = "TeamsheetDataBuild/1.0 (football trivia game data build)"
MIN_LINKS, MIN_BORN, LIMIT, BATCH = 20, 1962, 5000, 400
OUT = Path(__file__).with_name("wikidata-footballers.json")


def sparql(query: str, accept: str) -> str:
    data = urllib.parse.urlencode({"query": query}).encode()
    req = urllib.request.Request(ENDPOINT, data=data, headers={"User-Agent": UA, "Accept": accept})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=90) as r:
                return r.read().decode("utf-8")
        except Exception as e:  # 429 / 5xx / timeout: back off and retry
            wait = 5 * (attempt + 1)
            print(f"  retry in {wait}s ({e})", file=sys.stderr)
            time.sleep(wait)
    raise SystemExit("Wikidata query kept failing")


# Famous for something else but recorded with a playing position, so the occupation rule can't catch them.
# (Footballers who later went into politics, e.g. George Weah or Romário, stay.)
EXCLUDE = {"Q57641"}  # Viktor Orbán

# Occupations that still mean "a football person" (player, manager, coach, referee).
FOOTBALL_OCCUPATIONS = {"Q937857", "Q628099", "Q41583", "Q859528", "Q202648"}


def footballers_only(qids: list[str]) -> set[str]:
    """Drops celebrities/politicians who merely played (e.g. Viktor Orbán, Jason Statham): a person is excluded only
    if they have NO playing position (P413) AND some non-football occupation. Real players whose Wikidata entry
    just lacks a position (e.g. Mkhitaryan) are kept."""
    keep: set[str] = set()
    for i in range(0, len(qids), 500):
        values = " ".join(f"wd:{q}" for q in qids[i:i + 500])
        res = json.loads(sparql(f"""
            SELECT ?p (COUNT(DISTINCT ?pos) AS ?npos) (GROUP_CONCAT(DISTINCT ?occ; separator=" ") AS ?occs) WHERE {{
              VALUES ?p {{ {values} }}
              OPTIONAL {{ ?p wdt:P413 ?pos }}
              OPTIONAL {{ ?p wdt:P106 ?occ }}
            }} GROUP BY ?p
            """, "application/sparql-results+json"))
        for b in res["results"]["bindings"]:
            q = b["p"]["value"].rsplit("/", 1)[1]
            occs = {o.rsplit("/", 1)[1] for o in b.get("occs", {}).get("value", "").split() if o}
            if int(b["npos"]["value"]) > 0 or occs <= FOOTBALL_OCCUPATIONS:
                keep.add(q)
        time.sleep(1)
    return keep


def write(players: list[dict]) -> None:
    OUT.write_text(json.dumps({
        "_meta": {
            "source": "Wikidata (CC0), https://query.wikidata.org",
            "query": f"P106=Q937857, sitelinks>={MIN_LINKS}, born>={MIN_BORN}, top {LIMIT} by sitelinks; "
                     "excluding people with no playing position (P413) AND a non-football occupation",
            "fame": "number of Wikipedia/Wikimedia sitelinks (a popularity proxy)",
        },
        "players": players,
    }, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {OUT} with {len(players)} players (fame {players[0]['fame']} .. {players[-1]['fame']})")


def main() -> None:

    rows = list(csv.DictReader(io.StringIO(sparql(f"""
        SELECT ?p ?links WHERE {{ ?p wdt:P106 wd:Q937857 ; wikibase:sitelinks ?links . FILTER(?links >= {MIN_LINKS}) }}
        """, "text/csv"))))
    links = {r["p"].rsplit("/", 1)[1]: int(r["links"]) for r in rows}
    print(f"step 1: {len(links)} footballers with >= {MIN_LINKS} sitelinks")

    qids = sorted(links, key=lambda q: -links[q])
    info: dict[str, dict] = {}
    for i in range(0, len(qids), BATCH):
        values = " ".join(f"wd:{q}" for q in qids[i:i + BATCH])
        res = json.loads(sparql(f"""
            SELECT ?p ?label (MIN(YEAR(?dob)) AS ?born) (GROUP_CONCAT(DISTINCT ?alt; separator="|") AS ?alts) WHERE {{
              VALUES ?p {{ {values} }}
              ?p rdfs:label ?label . FILTER(LANG(?label) = "en")
              OPTIONAL {{ ?p wdt:P569 ?dob }}
              OPTIONAL {{ ?p skos:altLabel ?alt . FILTER(LANG(?alt) = "en") }}
            }} GROUP BY ?p ?label
            """, "application/sparql-results+json"))
        for b in res["results"]["bindings"]:
            q = b["p"]["value"].rsplit("/", 1)[1]
            born = b.get("born", {}).get("value")
            alts = [a for a in b.get("alts", {}).get("value", "").split("|") if a]
            info[q] = {"label": b["label"]["value"], "born": int(born) if born else None, "alts": alts}
        print(f"step 2: {min(i + BATCH, len(qids))}/{len(qids)}")
        time.sleep(1)

    born_ok = [q for q in qids if q in info and info[q]["born"] and info[q]["born"] >= MIN_BORN]
    keep = footballers_only(born_ok[:LIMIT + 300])   # only the top slice matters
    dropped = [info[q]["label"] for q in born_ok[:LIMIT + 300] if q not in keep]
    print(f"dropped {len(dropped)} non-footballers: {dropped}")
    players = [
        {"qid": q, "label": info[q]["label"], "fame": links[q], "born": info[q]["born"], "alts": info[q]["alts"]}
        for q in born_ok if q in keep and q not in EXCLUDE
    ][:LIMIT]
    write(players)


if __name__ == "__main__":
    main()
