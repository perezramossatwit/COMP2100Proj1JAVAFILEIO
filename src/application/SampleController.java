
package application ;

import javafx.event.ActionEvent ;
import javafx.fxml.FXML ;
import javafx.scene.control.Button ;
import javafx.scene.control.TextArea ;
import javafx.scene.control.TextField ;


public class SampleController
{

    @FXML
    private Button send ;

    @FXML
    private TextField userMessage ;

    @FXML
    private TextArea messageBox ;


    public void initialize()
    {

        messageBox.setEditable( false ) ;

    }


    @FXML
    private void messageSend( ActionEvent event )
    {

        String userTyped = userMessage.getText() ;
        messageBox.appendText( userTyped ) ;
        messageBox.appendText( "\n" ) ;
        userMessage.clear() ;

    }


}
