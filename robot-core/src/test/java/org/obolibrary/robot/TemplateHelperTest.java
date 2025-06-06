package org.obolibrary.robot;

import static org.junit.Assert.*;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxClassExpressionParser;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

/**
 * Tests template convenience methods.
 *
 * @author <a href="mailto:rctauber@gmail.com">Becky Tauber</a>
 */
public class TemplateHelperTest extends CoreTest {

  private QuotedEntityChecker checker;
  private static final OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();

  /**
   * Set up the checker loaded with the input ontology
   *
   * @throws IOException on issue loading ontology
   */
  @Before
  public void setUp() throws IOException {
    OWLOntology inputOntology = loadOntology("/uberon.owl");

    checker = new QuotedEntityChecker();
    checker.setIOHelper(new IOHelper());
    checker.addProvider(new SimpleShortFormProvider());
    checker.addProperty(dataFactory.getRDFSLabel());
    if (inputOntology != null) {
      checker.addAll(inputOntology);
    }
  }

  /**
   * Tests getting a set of annotation properties from template values.
   *
   * @throws Exception on issue getting annotation properties
   */
  @Test
  public void testGetAnnotationProperties() throws Exception {
    String value = "oboInOwl:inSubset|oboInOwl:id";
    String split = "|";
    Set<OWLAnnotationProperty> properties =
        TemplateHelper.getAnnotationProperties(checker, value, split, 0);

    OWLAnnotationProperty p1 = checker.getOWLAnnotationProperty("oboInOwl:inSubset");
    OWLAnnotationProperty p2 = checker.getOWLAnnotationProperty("oboInOwl:id");
    Set<OWLAnnotationProperty> propMatch = Sets.newHashSet(p1, p2);

    assertEquals(propMatch, properties);
  }

  /**
   * Tests getting various types of OWLAnnotations from strings.
   *
   * @throws Exception on issue getting annotations
   */
  @Test
  public void testGetAnnotations() throws Exception {
    // String
    String template = "A rdfs:label";
    String value = "anatomical cluster";
    Set<OWLAnnotation> annotations =
        TemplateHelper.getAnnotations("", checker, template, value, 0, 0);

    OWLAnnotationProperty p = checker.getOWLAnnotationProperty("rdfs:label");
    OWLLiteral lit = dataFactory.getOWLLiteral(value);
    OWLAnnotation annMatch = dataFactory.getOWLAnnotation(p, lit);

    for (OWLAnnotation a : annotations) {
      assertEquals(annMatch, a);
    }

    // Language
    template = "AL rdfs:label@en";
    annotations = TemplateHelper.getAnnotations("", checker, template, value, 0, 0);

    lit = dataFactory.getOWLLiteral(value, "en");
    annMatch = dataFactory.getOWLAnnotation(p, lit);

    for (OWLAnnotation a : annotations) {
      assertEquals(annMatch, a);
    }

    // Typed
    template = "AT rdfs:label^^xsd:string";
    annotations = TemplateHelper.getAnnotations("", checker, template, value, 0, 0);

    OWLDatatype dt = checker.getOWLDatatype("xsd:string");
    lit = dataFactory.getOWLLiteral(value, dt);
    annMatch = dataFactory.getOWLAnnotation(p, lit);

    for (OWLAnnotation a : annotations) {
      assertEquals(annMatch, a);
    }

    // IRI
    template = "AI rdfs:seeAlso";
    value = "http://robot.obolibrary.org/";
    annotations = TemplateHelper.getAnnotations("", checker, template, value, 0, 0);

    p = checker.getOWLAnnotationProperty("rdfs:seeAlso");
    IRI iri = IRI.create(value);
    annMatch = dataFactory.getOWLAnnotation(p, iri);

    for (OWLAnnotation a : annotations) {
      assertEquals(annMatch, a);
    }

    // Annotation Properties
    template = "AP";
    value = "obo:p1 'A'";
    annotations = TemplateHelper.getAnnotations("", checker, template, value, 0, 0);

    OWLLiteral o;
    Set<OWLAnnotation> expected = new HashSet<>();
    p = checker.getOWLAnnotationProperty("obo:p1");
    o = dataFactory.getOWLLiteral("A");
    expected.add(dataFactory.getOWLAnnotation(p, o));

    // Annotation Properties with SPLIT
    template = "AP SPLIT=|";
    value =
        "obo:p1 'A'"
            + "|obo:p2 \"B\"@en"
            + "|obo:p3 '3'^^xsd:integer"
            + "|obo:p4 obo:curie"
            + "|obo:p5 <http://ex.com>";
    annotations = TemplateHelper.getAnnotations("", checker, template, value, 0, 0);

    expected = new HashSet<>();
    p = checker.getOWLAnnotationProperty("obo:p1");
    o = dataFactory.getOWLLiteral("A");
    expected.add(dataFactory.getOWLAnnotation(p, o));
    p = checker.getOWLAnnotationProperty("obo:p2");
    o = dataFactory.getOWLLiteral("B", "en");
    expected.add(dataFactory.getOWLAnnotation(p, o));
    p = checker.getOWLAnnotationProperty("obo:p3");
    OWLDatatype datatype = checker.getOWLDatatype("xsd:integer");
    o = dataFactory.getOWLLiteral("3", datatype);
    expected.add(dataFactory.getOWLAnnotation(p, o));
    p = checker.getOWLAnnotationProperty("obo:p4");
    iri = checker.getIRI("obo:curie", true);
    expected.add(dataFactory.getOWLAnnotation(p, iri));
    p = checker.getOWLAnnotationProperty("obo:p5");
    iri = IRI.create("http://ex.com");
    expected.add(dataFactory.getOWLAnnotation(p, iri));

    assertEquals(expected, annotations);
  }

  /**
   * Tests getting a class expression from a template string and value.
   *
   * @throws Exception on issue getting class expressions
   */
  @Test
  public void testGetClassExpressions() throws Exception {
    ManchesterOWLSyntaxClassExpressionParser parser =
        new ManchesterOWLSyntaxClassExpressionParser(dataFactory, checker);

    // Check CURIE
    String template = "C part_of some %";
    String value = "obo:UBERON_0000467";
    Set<OWLClassExpression> expressions =
        TemplateHelper.getClassExpressions("", parser, template, value, 0, 0);
    OWLObjectProperty p = checker.getOWLObjectProperty("part_of");
    if (p == null) {
      fail("'part_of' property not found by checker");
    }
    OWLClass c = checker.getOWLClass("anatomical system");
    if (c == null) {
      fail("'anatomical system' class not found by checker");
    }
    OWLClassExpression exprMatch = dataFactory.getOWLObjectSomeValuesFrom(p, c);

    if (expressions.size() != 1) {
      fail(String.format("Expected exactly 1 expression, got %d", expressions.size()));
    }
    for (OWLClassExpression expr : expressions) {
      assertEquals(exprMatch.toString(), expr.toString());
    }

    // Check raw HTTP IRI
    value = "http://purl.obolibrary.org/obo/UBERON_0000467";
    expressions = TemplateHelper.getClassExpressions("", parser, template, value, 0, 0);
    if (expressions.size() != 1) {
      fail(String.format("Expected exactly 1 expression, got %d", expressions.size()));
    }
    for (OWLClassExpression expr : expressions) {
      assertEquals(exprMatch.toString(), expr.toString());
    }

    // Check that undeclared prefix fails
    try {
      value = "UNKNOWN:1234";
      expressions = TemplateHelper.getClassExpressions("", parser, template, value, 0, 0);
      fail("CURIE with undeclared prefix should not be parsed as Manchester expression");
    } catch (Exception e) {
    }

    // Check that gibberish fails
    try {
      value = "http.purl.obolibrary.org/obo/UBERON_0000467";
      expressions = TemplateHelper.getClassExpressions("", parser, template, value, 0, 0);
      fail("Gibberish value should not be parsed as Manchester expression");
    } catch (Exception e) {
    }
  }

  /**
   * Tests getting a data property expression from a template string and value.
   *
   * @throws Exception on issue getting data properties
   */
  @Test
  public void testGetDataPropertyExpressions() throws Exception {
    String template = "P %";
    String value = "UBERON:8888888";
    Set<OWLDataPropertyExpression> expressions =
        TemplateHelper.getDataPropertyExpressions("", checker, template, value, 0, 0);

    OWLDataProperty p = checker.getOWLDataProperty("height");
    if (p == null) {
      fail("'height' property not found by checker");
    }

    if (expressions.size() != 1) {
      fail(String.format("Expected exactly 1 expression, got %d", expressions.size()));
    }
    for (OWLDataPropertyExpression expr : expressions) {
      assertEquals(p.toString(), expr.toString());
    }
  }

  /**
   * Tests getting a set of datatypes from template values.
   *
   * @throws Exception on issue getting datatypes
   */
  @Test
  public void testGetDatatypes() throws Exception {
    String value = "xsd:string|xsd:boolean";
    String split = "|";
    Set<OWLDatatype> datatypes = TemplateHelper.getDatatypes("", checker, value, split, 0, 0);

    OWLDatatype dt1 = checker.getOWLDatatype("xsd:string");
    OWLDatatype dt2 = checker.getOWLDatatype("xsd:boolean");
    Set<OWLDatatype> dtMatch = Sets.newHashSet(dt1, dt2);

    assertEquals(dtMatch, datatypes);
  }

  /**
   * Tests getting an object property expression from a template string and value.
   *
   * @throws Exception on issue getting object property expressions
   */
  @Test
  public void testGetObjectPropertyExpressions() throws Exception {
    String template = "P inverse %";
    String value = "obo:BFO_0000050";
    Set<OWLObjectPropertyExpression> expressions =
        TemplateHelper.getObjectPropertyExpressions("", checker, template, value, 0, 0);

    OWLObjectProperty p = checker.getOWLObjectProperty("part_of");
    if (p == null) {
      fail("'part_of' property not found by checker");
    }
    OWLObjectPropertyExpression exprMatch = dataFactory.getOWLObjectInverseOf(p);

    if (expressions.size() != 1) {
      fail(String.format("Expected exactly 1 expression, got %d", expressions.size()));
    }
    for (OWLObjectPropertyExpression expr : expressions) {
      assertEquals(exprMatch.toString(), expr.toString());
    }
  }

  /**
   * Test annotation templates.
   *
   * @throws Exception if annotation properties cannot be found
   */
  @Test
  public void testTemplateStrings() throws Exception {
    Set<OWLAnnotation> anns;
    OWLAnnotation ann = null;
    QuotedEntityChecker checker = new QuotedEntityChecker();
    checker.setIOHelper(new IOHelper());

    anns = TemplateHelper.getStringAnnotations(checker, "A rdfs:label", null, "bar", 0);
    for (OWLAnnotation a : anns) {
      ann = a;
    }
    assertEquals("Annotation(rdfs:label \"bar\"^^xsd:string)", ann.toString());

    anns =
        TemplateHelper.getTypedAnnotations(
            "", checker, "AT rdfs:label^^xsd:integer", null, "1", 0, 0);
    for (OWLAnnotation a : anns) {
      ann = a;
    }
    assertEquals("Annotation(rdfs:label \"1\"^^xsd:integer)", ann.toString());

    anns = TemplateHelper.getLanguageAnnotations(checker, "AL rdfs:label@en", null, "bar", 0);
    for (OWLAnnotation a : anns) {
      ann = a;
    }
    assertEquals("Annotation(rdfs:label \"bar\"@en)", ann.toString());

    ann =
        TemplateHelper.getIRIAnnotation(checker, "AI rdfs:label", IRI.create("http://bar.com"), 0);
    assertEquals("Annotation(rdfs:label <http://bar.com>)", ann.toString());
  }

  @Test
  public void testGetSplitValuesShouldSplitOnDelimiterCharacter() {
    String[] values = TemplateHelper.getSplitValues("A,B,C", ",");
    String[] expectedValues = {"A", "B", "C"};
    assertArrayEquals(expectedValues, values);
  }

  @Test
  public void testGetSplitValuesShouldSplitOnPipeCharacter() {
    String[] values = TemplateHelper.getSplitValues("A|B|C", "|");
    String[] expectedValues = {"A", "B", "C"};
    assertArrayEquals(expectedValues, values);
  }

  @Test
  public void testGetSplitValuesShouldSplitOnNewlineCharacterRepresentation() {
    String[] values = TemplateHelper.getSplitValues("A\nB\nC", "\\n");
    String[] expectedValues = {"A", "B", "C"};
    assertArrayEquals(expectedValues, values);
  }

  @Test
  public void testSplitEmptyString() {
    String[] values = TemplateHelper.getSplitValues("", ",");
    assertArrayEquals(new String[] {""}, values);
  }
}
