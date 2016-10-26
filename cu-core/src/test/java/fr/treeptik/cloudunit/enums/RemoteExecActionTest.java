package fr.treeptik.cloudunit.enums;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by nicolas on 09/08/2016.
 */
public class RemoteExecActionTest {

    @Test
    public void getCommandWithParam() throws Exception {
        Map<String, String> kv = new HashMap<>();
        kv.put("CU_USER", "johndoe");
        kv.put("CU_PASSWORD", "abc2015");
        String resultat = RemoteExecAction.ADD_USER.getCommand(kv);
        Assert.assertTrue(resultat.contains("johndoe") && resultat.contains("abc2015"));
    }

}