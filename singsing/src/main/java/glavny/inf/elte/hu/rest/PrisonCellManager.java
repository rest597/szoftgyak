package glavny.inf.elte.hu.rest;

import java.security.Principal;
import java.sql.Timestamp;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import glavny.inf.elte.hu.data.Area;
import glavny.inf.elte.hu.data.AreaRepository;
import glavny.inf.elte.hu.data.AuditLog;
import glavny.inf.elte.hu.data.AuditLogRepository;
import glavny.inf.elte.hu.data.Prisoncell;
import glavny.inf.elte.hu.data.PrisoncellRepository;

@RestController
@RequestMapping("prisoncell")
@Transactional
public class PrisonCellManager {
    private static Logger log = LoggerFactory.getLogger(PrisonCellManager.class);

    @Autowired
    private PrisoncellRepository prisoncellRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AreaRepository areaRepository;

    @GetMapping("/")
    public ResponseEntity<List<Prisoncell>> getPrisonCells(Authentication auth) {
        return new ResponseEntity<List<Prisoncell>>(prisoncellRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("/free_space")
    public ResponseEntity<List<Prisoncell>> getPrisonCellsWithFreeSpace(Authentication auth) {
        return new ResponseEntity<List<Prisoncell>>(prisoncellRepository.findCellWithFreeSpace(), HttpStatus.OK);
    }

    @PostMapping("/auditlog")
    public ResponseEntity<Void> createLogCSVFile(
            @RequestParam(value = "fileName", defaultValue = "D:/log.cs") String fileName, Authentication auth) {
        HttpHeaders headers = new HttpHeaders();
        return new ResponseEntity<Void>(headers, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prisoncell> getPrisonCell(@PathVariable("id") Integer id, Authentication auth) {
        Prisoncell result = prisoncellRepository.getOne(id);
        result.getPrisoners();
        return new ResponseEntity<Prisoncell>(result, HttpStatus.OK);
    }

    @PostMapping("/new")
    public ResponseEntity<Void> createPrisonCell(@RequestBody Prisoncell c, UriComponentsBuilder builder,
            Principal principal) {
        boolean flag = true;

        Area area = areaRepository.getOne(c.getAreaId());
        c.setArea(area);

        prisoncellRepository.save(c);
        if (flag == false) {
            return new ResponseEntity<Void>(HttpStatus.CONFLICT);
        }

        auditLogRepository.save(
                new AuditLog(principal.getName(), new Timestamp(System.currentTimeMillis()), "CREATE", c.toString()));
        HttpHeaders headers = new HttpHeaders();
        // headers.setLocation(builder.path("/{id}").buildAndExpand(c.getId()).toUri());
        return new ResponseEntity<Void>(headers, HttpStatus.CREATED);
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updatePrisonCell(@RequestBody Prisoncell c, UriComponentsBuilder builder) {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        auditLogRepository.save(new AuditLog(user, new Timestamp(System.currentTimeMillis()), "MODIFY", c.toString()));
        Area area = areaRepository.getOne(c.getAreaId());
        c.setArea(area);

        prisoncellRepository.save(c);

        HttpHeaders headers = new HttpHeaders();
        return new ResponseEntity<Void>(headers, HttpStatus.CREATED);
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deletePrisonCell(@RequestBody Prisoncell c, UriComponentsBuilder builder,
            Principal principal) {
        boolean flag = true;
        if (c.getPrisoners().size() > 0) {
            return new ResponseEntity<Void>(HttpStatus.CONFLICT);
        }

        auditLogRepository.save(
                new AuditLog(principal.getName(), new Timestamp(System.currentTimeMillis()), "DELETE", c.toString()));
        prisoncellRepository.delete(c);

        if (flag == false) {
            return new ResponseEntity<Void>(HttpStatus.CONFLICT);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path("/{id}").buildAndExpand(c.getId()).toUri());
        return new ResponseEntity<Void>(headers, HttpStatus.CREATED);
    }
}
