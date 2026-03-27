import com.huggingface.transformers.Transformer;

public class ModelCheck {
    public static void main(String[] args) {
        Transformer transformer = new Transformer();
        String activeModel = transformer.getActiveModel();
        System.out.println("The currently active model is: " + activeModel);
    }
}