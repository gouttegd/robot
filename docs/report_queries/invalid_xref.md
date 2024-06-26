# Invalid Xref

**Problem:** A database_cross_reference is not in CURIE format.

**Solution:** Replace the reference with a [CURIE](https://www.w3.org/TR/2010/NOTE-curie-20101216/).

```sparql
PREFIX oboInOwl: <http://www.geneontology.org/formats/oboInOwl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT DISTINCT ?entity ?property ?value WHERE {
  VALUES ?property {oboInOwl:hasDbXref}
  ?entity ?property ?value .
  FILTER (!regex(?value, "^[A-Za-z_][A-Za-z0-9_.-]*[A-Za-z0-9_]:[^\\s]+$"))
  FILTER (!isBlank(?entity))
}
ORDER BY ?entity
```
