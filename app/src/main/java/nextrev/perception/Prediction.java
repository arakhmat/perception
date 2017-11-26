package nextrev.perception;

public class Prediction {

    private int value;
    private String info;

    public Prediction() {
        this.value = -1;
        this.info = "Loading...";
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
