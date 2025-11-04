
package application ;

import javafx.application.Application ;
import javafx.fxml.FXMLLoader ;
import javafx.scene.Scene ;
import javafx.scene.layout.BorderPane ;
import javafx.stage.Stage ;


public class Main extends Application
    {

    @Override
    public void start( final Stage primaryStage )
        {

        try
            {
            final BorderPane root = (BorderPane) FXMLLoader.load( getClass().getResource( "Sample.fxml" ) ) ;
            final Scene scene = new Scene( root, 400, 400 ) ;
            scene.getStylesheets()
                 .add( getClass().getResource( "application.css" )
                                 .toExternalForm() ) ;
            primaryStage.setScene( scene ) ;
            primaryStage.show() ;
            }
        catch ( final Exception e )
            {
            e.printStackTrace() ;
            }

        }


    public static void main( final String[] args )
        {

        launch( args ) ;

        }

    }
