package com.singhand.sr.graphservice.bizservice.suite;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
    "com.singhand.sr.graphservice.bizservice.unit",
    "com.singhand.sr.graphservice.bizservice.integration"
})
public class AllTests {

}
