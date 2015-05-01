public class Mia {
    public void f(int x, int y) {
    	assert (x >= 0) : "Oh";
    	assert (y < 0);
    	f(x-1,y-2);
    }

    public int g(float f) {
    	assert (true);
    	f(3,4);
    	return (int)f;
    }

    public Mia h(float g) {
    	f(1,3);
    	int y;
    	y = g(2.0f);
    	return null;
    }
    
    public double i(double x) {
    	Mia z = h((float)x);
    	i(x);
    	j(x);
    	return x;
    }
    
    public double j(double x) {
    	i(x);
    	return x+1;
    }
}

