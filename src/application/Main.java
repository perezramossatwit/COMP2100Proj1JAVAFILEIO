
package application ;

import javafx.application.Application ;
import javafx.fxml.FXMLLoader ;
import javafx.scene.Scene ;
import javafx.scene.layout.BorderPane ;
import javafx.stage.Stage ;


public class Main extends Application
{

    @Override
    public void start( Stage primaryStage )
    {

        try
        {
            FXMLLoader loader = new FXMLLoader( getClass().getResource( "Sample.fxml" ) ) ;
            loader.setController( new SampleController() ) ;
            BorderPane root = (BorderPane) FXMLLoader.load( getClass().getResource( "Sample.fxml" ) ) ;
            Scene scene = new Scene( root, 600, 600 ) ;
            scene.getStylesheets()
                 .add( getClass().getResource( "application.css" )
                                 .toExternalForm() ) ;
            primaryStage.setScene( scene ) ;
            primaryStage.setMaximized( true ) ;
            primaryStage.centerOnScreen() ;
            primaryStage.show() ;
        }
        catch ( Exception e )
        {
            e.printStackTrace() ;
        }

    }


    public static void main( String[] args )
    {

        launch( args ) ;

    }

}
