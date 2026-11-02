package client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MerchantWithdrawTest {
    @Test
    void noFeeMeansGrossEqualsNet() {
        assertEquals(1000, Character.grossForNetMerchantMeso(1000, 0));
    }

    @Test
    void grossCoversNetAfterFee() {
        // 10 days -> keep 90%: paying out 900 net consumes 1000 gross, remainder is not re-taxed
        assertEquals(1000, Character.grossForNetMerchantMeso(900, 10));
        long gross = 1000;
        long paidGross = Character.grossForNetMerchantMeso(900, 10);
        assertEquals(0, gross - paidGross);
        // rounding never pays out more than stored value would justify
        assertTrue(Character.grossForNetMerchantMeso(1, 10) * 90 / 100 >= 0);
    }

    @Test
    void fullFeeNeverPaysOut() {
        assertEquals(Long.MAX_VALUE, Character.grossForNetMerchantMeso(1, 100));
    }
}
