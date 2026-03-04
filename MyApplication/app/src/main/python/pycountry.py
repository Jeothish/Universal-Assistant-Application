import pycountry
def country_code(name: str) -> str: #country name to code for news api

    try:
        if name.lower() in UK_VARIANTS:
            return "gb"
        elif name.lower() == "america":
            return "us"
        else:
            code = pycountry.countries.lookup(name)
            return code.alpha_2.lower()
    except LookupError:
        return "wo"  # worldwide