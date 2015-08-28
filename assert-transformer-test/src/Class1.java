
import es.ucm.asserttransformer.annotations.AssertTransform;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author manuel
 */

public class Class1 {
    
    @AssertTransform
    Class1(int x1, double x2) {
        assert (x1 <= x2);
        System.out.println(x1 + x2);
    }
    
    @AssertTransform
    public int f(int x) {
        assert (x >= 0);
        return x + 2;
    }
    
    public boolean g(int x) {
        int y = 1 + f(x);
        System.out.println(y);
        boolean p;
        if (false)  p = g(f(10)); 
        if (g(1)) return true;
        while (g(1)) { int z = 0; }
        for (int j = 0; j < f(4); j++) {
            j++;
        }
        return y == 3;
    }
    
    public void h() {
        boolean b = g(1);
        if (b) {
            System.out.println("Yes!");
        }
    }
    
    public void i() {
        Class2 c = new Class2();
    }
    
    public static class Class2 {
        public Class2() {
            Class1 cl1 = new Class1(1, 1.0);
            if (1 <= 0) return;
            cl1.h();
        }
    }
}
