package healthsystem;

import es.ucm.asserttransformer.annotations.AssertTransform;
import es.ucm.asserttransformer.annotations.CopyOfMethod;
import es.ucm.asserttransformer.maybe.Maybe;

public class DonorHealth {
    DonorData d;

    public DonorHealth(DonorData d) {
        this.d = d;
    }

    @AssertTransform
    public boolean checkHealth() {
        assert (d.getAge()) >= 18 : "age not allowed (change of law!)";
        return ((d.getHighsystolic()) < 180) && ((d.getHighdiastolic()) > 100);
    }

}

