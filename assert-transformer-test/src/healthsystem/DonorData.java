package healthsystem;


public class DonorData {
    private int id;

    private int age = 18;

    private int highsystolic;

    private int highdiastolic;

    private boolean rejected;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getHighsystolic() {
        return highsystolic;
    }

    public int getHighdiastolic() {
        return highdiastolic;
    }

    public boolean getRejected() {
        return rejected;
    }

    public void setRejected(boolean rejected) {
        this.rejected = rejected;
    }
}

