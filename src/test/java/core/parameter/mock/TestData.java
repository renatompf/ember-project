package core.parameter.mock;

public class TestData {
    private String name;
    private int age;

    // Default constructor needed for deserialization
    public TestData() {}

    public TestData(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Plain text constructor
    public TestData(String text) {
        this.name = text;
        this.age = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}