package healthsystem;

import es.ucm.asserttransformer.annotations.AssertTransform;
import es.ucm.asserttransformer.annotations.CopyOfMethod;
import es.ucm.asserttransformer.maybe.Maybe;

public class BloodDonor {
    @AssertTransform
    public static boolean canGiveBlood(DonorData d) {
        boolean canGiveBlood;
        assert !(d.getRejected()) : "rejected previously!";
        if ((d.getAge()) >= 0) {
            canGiveBlood = new DonorHealth(d).checkHealth();
        } else
            canGiveBlood = false;
        
        return canGiveBlood;
    }


    public static void main(String[] args) {
        DonorData donorData0 = new DonorData();
        donorData0.setRejected(true);
        Boolean canGiveBlood = BloodDonor.canGiveBlood(donorData0);
        System.out.println(canGiveBlood);
    }
}

