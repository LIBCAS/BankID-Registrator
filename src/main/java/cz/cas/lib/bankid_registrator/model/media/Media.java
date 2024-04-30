package cz.cas.lib.bankid_registrator.model.media;

import cz.cas.lib.bankid_registrator.dto.PatronDTO;
import javax.persistence.*;

@Entity
@Table(name = "media")
public class Media {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String path;

    @ManyToOne
    @JoinColumn(name = "patron_dto_id")
    private PatronDTO patronDTO;

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPatronDTO(PatronDTO patronDTO) {
        this.patronDTO = patronDTO;
    }

    public PatronDTO getPatronDTO() {
        return patronDTO;
    }
}
