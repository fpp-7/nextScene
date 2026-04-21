"""
Testes unitários — Pré-processamento
"""

import pytest
import pandas as pd
from src.preprocessing.cleaner import _extract_year, _clean_title, _assign_era


def test_extract_year_normal():
    assert _extract_year("Toy Story (1995)") == 1995


def test_extract_year_no_year():
    assert _extract_year("Untitled Film") is None


def test_extract_year_edge():
    assert _extract_year("Movie (2023)") == 2023


def test_clean_title():
    assert _clean_title("Toy Story (1995)") == "Toy Story"
    assert _clean_title("The Dark Knight (2008)") == "The Dark Knight"


def test_assign_era():
    assert _assign_era(1940) == "classical"
    assert _assign_era(1970) == "new_hollywood"
    assert _assign_era(1990) == "blockbuster"
    assert _assign_era(2010) == "contemporary"
    assert _assign_era(2020) == "recent"
    assert _assign_era(None) == "unknown"
