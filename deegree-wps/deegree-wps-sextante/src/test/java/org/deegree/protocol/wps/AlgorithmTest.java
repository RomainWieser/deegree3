package org.deegree.protocol.wps;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import org.deegree.protocol.wps.client.WPSClient;
import org.deegree.protocol.wps.client.output.ExecutionOutput;
import org.deegree.protocol.wps.client.process.ProcessExecution;
import org.deegree.protocol.wps.client.process.Process;
import org.deegree.protocol.wps.client.process.execute.ExecutionOutputs;
import org.deegree.services.wps.provider.SextanteProcessProvider;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import es.unex.sextante.core.GeoAlgorithm;
import es.unex.sextante.core.OutputObjectsSet;
import es.unex.sextante.core.ParametersSet;
import es.unex.sextante.core.Sextante;
import es.unex.sextante.outputs.Output;
import es.unex.sextante.parameters.Parameter;

public class AlgorithmTest {

    private static final boolean ENABLED = true;

    private static Logger LOG = LoggerFactory.getLogger( AlgorithmTest.class );

    private LinkedList<TestAlgorithm> getAlgorithms() {
        LinkedList<TestAlgorithm> algs = new LinkedList<TestAlgorithm>();

        // only one algorithm
        // Sextante.initialize();
        // HashMap<String, GeoAlgorithm> sextanteAlgs = Sextante.getAlgorithms();
        // GeoAlgorithm geoAlg = sextanteAlgs.get( "geometriestopoints" );
        // TestAlgorithm testAlg = new TestAlgorithm( geoAlg );
        // testAlg.addAllInputData( getInputData( geoAlg ) );
        // algs.add( testAlg );

        // all vector algorithms
        GeoAlgorithm[] geoalgs = SextanteProcessProvider.getVectorLayerAlgorithms();
        for ( int i = 0; i < geoalgs.length; i++ ) {
            GeoAlgorithm geoAlg = geoalgs[i];

            if ( !geoAlg.getCommandLineName().equals( "cleanpointslayer" )
                 && !geoAlg.getCommandLineName().equals( "polylinestopolygons" )
                 && !geoAlg.getCommandLineName().equals( "pointcoordinates" )
                 && !geoAlg.getCommandLineName().equals( "removeholes" )
                 && !geoAlg.getCommandLineName().equals( "exportvector" )
                 && !geoAlg.getCommandLineName().equals( "polygonize" ) ) {

                TestAlgorithm testAlg = new TestAlgorithm( geoAlg );
                testAlg.addAllInputData( getInputData( geoAlg ) );
                algs.add( testAlg );

            }
        }

        return algs;
    }

    private LinkedList<LinkedList<ExampleData>> getInputData( GeoAlgorithm alg ) {

        // all input data
        LinkedList<LinkedList<ExampleData>> allData = new LinkedList<LinkedList<ExampleData>>();

        // example data in categories
        LinkedList<ExampleData> layers = getLayerInput();
        LinkedList<ExampleData> lines = getLinesInput();
        LinkedList<ExampleData> points = getPointsInput();

        // example data iterators
        Iterator<ExampleData> layersIterator = layers.iterator();
        Iterator<ExampleData> linesIterator = lines.iterator();
        Iterator<ExampleData> pointsIterator = points.iterator();
        Iterator<ExampleData> inputIterator = layers.iterator();

        // traverse input parameter of one execution
        ParametersSet paramSet = alg.getParameters();

        // traverse all example data
        boolean addMoreData = true;
        while ( addMoreData ) {

            // input data of one execution
            LinkedList<ExampleData> dataList = new LinkedList<ExampleData>();

            for ( int j = 0; j < paramSet.getNumberOfParameters(); j++ ) {
                Parameter param = paramSet.getParameter( j );

                // add a layer
                if ( param.getParameterName().equals( "LAYER" ) || param.getParameterName().equals( "INPUT" ) ) {

                    if ( layersIterator.hasNext() ) {
                        dataList.add( layersIterator.next() );
                    } else {
                        addMoreData = false;
                        break;
                    }

                } else {
                    // add lines
                    if ( param.getParameterName().equals( "LINES" ) ) {
                        if ( linesIterator.hasNext() ) {
                            dataList.add( linesIterator.next() );
                        } else {
                            addMoreData = false;
                            break;
                        }

                    } else {
                        // add points
                        if ( param.getParameterName().equals( "POINTS" ) ) {
                            if ( pointsIterator.hasNext() ) {
                                dataList.add( pointsIterator.next() );
                            } else {
                                addMoreData = false;
                                break;
                            }

                        } else {

                            // add nothing
                            LOG.error( "Unknown input parameter \"" + param.getParameterName()
                                       + "\" of SEXTANTE algorithm." );
                        }
                    }
                }
            }

            allData.add( dataList );
        }

        return allData;
    }

    private LinkedList<ExampleData> getLayerInput() {
        LinkedList<ExampleData> layers = new LinkedList<ExampleData>();

        layers.add( ExampleData.GML_31_MULTILPOLYGON );
        layers.add( ExampleData.GML_31_MULTILINESTRING );
        layers.add( ExampleData.GML_31_MULTIPOINT );
        layers.add( ExampleData.GML_31_POLYGON );
        layers.add( ExampleData.GML_31_LINESTRING );
        layers.add( ExampleData.GML_31_POINT );
        layers.add( ExampleData.GML_31_FEATURE_COLLECTION_MULTIPOLYGONS_1 );
        layers.add( ExampleData.GML_31_FEATURE_COLLECTION_MULTIPOLYGONS_2 );
        layers.add( ExampleData.GML_31_FEATURE_COLLECTION_MULTILINESTRINGS_1 );
        layers.add( ExampleData.GML_31_FEATURE_COLLECTION_LINESTRINGS_1 );
        layers.add( ExampleData.GML_31_FEATURE_COLLECTION_MULTIPOINTS_1 );
        layers.add( ExampleData.GML_31_FEATURE_COLLECTION_POINTS_1 );

        return layers;
    }

    private LinkedList<ExampleData> getLinesInput() {
        LinkedList<ExampleData> lines = new LinkedList<ExampleData>();
        // lines.add( ExampleData.GML_31_FEATURE_COLLECTION_MULTILINESTRINGS_1 );
        lines.add( ExampleData.GML_31_FEATURE_COLLECTION_LINESTRINGS_1 );
        return lines;
    }

    private LinkedList<ExampleData> getPointsInput() {
        LinkedList<ExampleData> points = new LinkedList<ExampleData>();
        points.add( ExampleData.GML_31_FEATURE_COLLECTION_POINTS_1 );
        return points;
    }

    @Test
    public void testAlgorithms() {
        if ( ENABLED ) {
            try {

                // client
                URL wpsURL = new URL(
                                      "http://localhost:9080/deegree-wps-demo/services?service=WPS&version=1.0.0&request=GetCapabilities" );
                WPSClient client = new WPSClient( wpsURL );
                Assert.assertNotNull( client );

                // all algorithms
                LinkedList<TestAlgorithm> algs = getAlgorithms();

                // traverse all algorithms
                for ( TestAlgorithm algTest : algs ) {

                    // geoalgorithm
                    GeoAlgorithm alg = algTest.getAlgorithm();

                    // input data
                    LinkedList<LinkedList<ExampleData>> allData = algTest.getAllInputData();
                    for ( LinkedList<ExampleData> data : allData ) {

                        // set input data for one execution
                        ParametersSet paramSet = alg.getParameters();
                        if ( data.size() > 0 )
                            if ( data.size() == paramSet.getNumberOfParameters() ) {
                                Process process = client.getProcess( alg.getCommandLineName() );
                                ProcessExecution execution = process.prepareExecution();

                                Iterator<ExampleData> it = data.iterator();

                                for ( int j = 0; j < paramSet.getNumberOfParameters(); j++ ) {
                                    ExampleData currentData = it.next();

                                    Parameter param = paramSet.getParameter( j );

                                    String inputIndentifier = param.getParameterName();

                                    LOG.info( "Testing '" + alg.getCommandLineName() + "' algorithm with "
                                              + currentData.getFilename() );

                                    execution.addXMLInput( inputIndentifier, null, currentData.getURL(), false,
                                                           currentData.getMimeType(), currentData.getEncoding(),
                                                           currentData.getSchema() );
                                }

                                // set all output parameters
                                OutputObjectsSet outputSet = alg.getOutputObjects();
                                for ( int j = 0; j < outputSet.getOutputObjectsCount(); j++ ) {
                                    Output outp = outputSet.getOutput( j );

                                    String outputIdentifier = outp.getName();

                                    execution.addOutput( outputIdentifier, null, null, false, "text/xml", "UTF-8",
                                                         "http://schemas.opengis.net/gml/3.1.1/base/geometryComplexes.xsd" );
                                }

                                ExecutionOutputs outputs = execution.execute();
                                ExecutionOutput[] allOutputs = outputs.getAll();

                                // check number of output output objects
                                Assert.assertTrue( allOutputs.length > 0 );

                                // LOG.info( " '" + alg.getCommandLineName() + "' has " + allOutputs.length
                                // + " ouput objects." );

                            } else {
                                LOG.error( "Wrong number of input data." );
                            }

                    }
                }

            } catch ( Throwable t ) {
                LOG.error( t.getMessage(), t );
                Assert.fail( t.getLocalizedMessage() );
            }
        }
    }

    //
    // public void testVectorLayerAlgorithms()
    // throws OWSException, IOException, XMLStreamException {
    // if ( ENABLED ) {
    // try {
    // URL wpsURL = new URL(
    // "http://localhost:8080/deegree-wps-demo/services?service=WPS&version=1.0.0&request=GetCapabilities" );
    //
    // WPSClient client = new WPSClient( wpsURL );
    //
    // Assert.assertNotNull( client );
    //
    // File gmlPoints = new File( AlgorithmTest.class.getResource( "GML31_MultiPoint.xml" ).getPath() );
    // File gmlLines = new File( AlgorithmTest.class.getResource( "GML31_MultiLineString.xml" ).getPath() );
    // File gmlPolygons = new File( AlgorithmTest.class.getResource( "GML31_MultiPolygon.xml" ).getPath() );
    //
    // // Process process = client.getProcess( "centroids", null );
    // // ProcessExecution execution = process.prepareExecution();
    // // execution.addXMLInput( "LAYER", null, gmlGeometry.toURI().toURL(), false, "text/xml", "UTF-8",
    // // "http://schemas.opengis.net/gml/3.1.1/base/geometryComplexes.xsd" );
    // //
    // // execution.addOutput( "RESULT", null, null, false, "text/xml", "UTF-8",
    // // "http://schemas.opengis.net/gml/3.1.1/base/geometryComplexes.xsd" );
    // //
    // // ExecutionOutputs response = execution.execute();
    // // Assert.assertNotNull( response );
    //
    // GeoAlgorithm[] algs = SextanteProcessProvider.getVectorLayerAlgorithms();
    //
    // // process all vector algorithms
    // for ( int i = 0; i < algs.length; i++ ) {
    // GeoAlgorithm alg = algs[i];
    //
    // Process process = client.getProcess( alg.getCommandLineName() );
    // ProcessExecution execution = process.prepareExecution();
    //
    // // add all inputs
    // ParametersSet paramSet = alg.getParameters();
    // for ( int j = 0; j < paramSet.getNumberOfParameters(); j++ ) {
    // Parameter param = paramSet.getParameter( j );
    //
    // String inputIndentifier = param.getParameterName();
    //
    // File geom = null;
    //
    // if ( inputIndentifier.equals( "POINTS" ) )
    // geom = gmlPoints;
    // else if ( inputIndentifier.equals( "LINES" ) )
    // geom = gmlLines;
    // else
    // geom = gmlLines;
    //
    // execution.addXMLInput( inputIndentifier, null, geom.toURI().toURL(), false, "text/xml",
    // "UTF-8",
    // "http://schemas.opengis.net/gml/3.1.1/base/geometryComplexes.xsd" );
    // }
    //
    // // set all output parameters
    // OutputObjectsSet outputSet = alg.getOutputObjects();
    // for ( int j = 0; j < outputSet.getOutputObjectsCount(); j++ ) {
    // Output outp = outputSet.getOutput( j );
    //
    // String outputIdentifier = outp.getName();
    //
    // execution.addOutput( outputIdentifier, null, null, false, "text/xml", "UTF-8",
    // "http://schemas.opengis.net/gml/3.1.1/base/geometryComplexes.xsd" );
    //
    // }
    //
    // ExecutionOutputs outputs = execution.execute();
    //
    // }
    //
    // // execution
    // // .addXMLInput("GMLInput", null, gmlPoints.toURI().toURL(),
    // // false, "text/xml", null,
    // // "http://schemas.opengis.net/gml/3.1.1/base/geometryComplexes.xsd");
    //
    // // execution.addOutput("CentroidSextante", null, null, true, null, null,
    // // null);
    //
    // // ExecutionOutputs outputs = execution.execute();
    //
    // // access individual output values
    // // ComplexOutput outputGeometry = (ComplexOutput) outputs.get(
    // // "CentroidSextante", null);
    //
    // // LiteralOutput out = outputs.getLiteral("Data", null);
    //
    // // XMLStreamReader xmlStream = outputGeometry.getAsXMLStream();
    //
    // // System.out.println(out.getValue());
    // } catch ( Throwable t ) {
    // LOG.error( t.getMessage(), t );
    // }
    // }
    // }
}
