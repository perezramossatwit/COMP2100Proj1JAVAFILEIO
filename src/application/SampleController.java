
package application ;

import javafx.event.ActionEvent ;
import javafx.fxml.FXML ;
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

        this.messageBox.setEditable( false ) ;

        }


    @FXML
    private void messageSend( ActionEvent event )
        {

        String userTyped = this.userMessage.getText() ;
        this.messageBox.appendText( userTyped ) ;
        this.messageBox.appendText( "\n" ) ;
        this.userMessage.clear() ;

        }


    }
