"""
Saída de console independente da codificação do sistema.

O código imprime setas, emojis e acentos. No Linux e no Docker o stdout é UTF-8
e tudo funciona; no console do Windows o padrão é cp1252, e um simples `→`
derruba o processo com UnicodeEncodeError no meio do pipeline de treino.

Chamar `enable_utf8_stdout()` no início dos executáveis resolve para todos os
`print` e handlers de logging que escrevem em stdout/stderr.
"""

import sys


def enable_utf8_stdout() -> None:
    """Força UTF-8 em stdout/stderr, substituindo o que não puder ser codificado."""
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure is not None:
            try:
                reconfigure(encoding="utf-8", errors="replace")
            except (ValueError, OSError):
                # Stream redirecionado ou já fechado: seguir sem UTF-8 é melhor
                # que impedir o processo de rodar.
                pass
