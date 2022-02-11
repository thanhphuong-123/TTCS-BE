package tran.tuananh.movie.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tran.tuananh.movie.Service.CountryService;
import tran.tuananh.movie.Table.DTO.CountryDTO;
import tran.tuananh.movie.Table.Response.Response;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping(value = "api/v1/country")
public class CountryController {

    @Autowired
    private CountryService countryService;

    @GetMapping(value = "/get-all")
    public Response getAll() {
        return countryService.getAll();
    }

    @PostMapping(value = "/save")
    public Response saveOrUpdate(@RequestBody CountryDTO dto) {
        return countryService.saveOrUpdate(dto);
    }

    @PostMapping(value = "/get-by-id")
    public Response getById(@RequestBody CountryDTO dto) {
        return countryService.getById(dto);
    }

    @PostMapping(value = "/delete")
    public Response delete(@RequestBody CountryDTO dto) {
        return countryService.delete(dto);
    }

}
