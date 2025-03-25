package com.singhand.sr.graphservice.bizshell.model.xts;

import java.io.Serial;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EvidenceTriple_v3 extends EvidenceTripleResult {

  @Serial
  private static final long serialVersionUID = 6732656419863824061L;

  @Data
  public static class KvEvidenceRelationBean {

    private String evid;

    private String contentHash;

    private Long publishTime;

    private Long createTime = System.currentTimeMillis();
  }

//  private List<KvEvidenceRelationBean> kvEviRelationBeans = new ArrayList<>();
}
