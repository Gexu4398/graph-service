package com.singhand.sr.graphservice.bizmodel.repository.jpa;

import com.singhand.sr.graphservice.bizmodel.model.jpa.Picture;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public interface PictureRepository extends BaseRepository<Picture, Long> {

  Set<Picture> findByUrl(String src);
}
