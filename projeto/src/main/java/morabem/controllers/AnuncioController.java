package morabem.controllers;

import morabem.domain.Anuncio;

import morabem.domain.Usuario;
import morabem.domain.relatorio.Exportar;
import morabem.domain.relatorio.ExportarStrategy;
import morabem.domain.relatorio.Relatorio;
import morabem.exceptions.AnuncioException;
import morabem.exceptions.ImovelException;
import morabem.services.AnuncioService;
import morabem.services.ImovelService;
import morabem.services.UsuarioService;
import morabem.utils.BuscaData;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class AnuncioController {

    @Autowired
    private ImovelService imovelService;

    @Autowired
    private AnuncioService anuncioService;


    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EntityManager entityManager;

    @GetMapping(path = "/imoveis/{imovel}/anunciar")
    public String cadastroAnuncioForm(@PathVariable(value = "imovel") String codImovel, Model model) throws ImovelException.ImovelNaoExiste {
        Anuncio anuncio = new Anuncio();
        anuncio.setImovel(imovelService.obterPorCodigo(codImovel));
        model.addAttribute("anuncio", anuncio);
        model.addAttribute("tipos", Anuncio.Tipo.values());
        return "novoAnuncio";
    }

    @PostMapping(path = "/imoveis/{imovel}/anunciar")
    public String cadastroAnuncioSubmit(Model model,
                                        @PathVariable(value = "imovel") String codImovel,
                                        @ModelAttribute Anuncio anuncio, HttpSession session) throws ImovelException.ImovelNaoExiste {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if (usuario == null) {
            model.addAttribute("error", "Sua sessão expirou.");
            return "redirect:/login";
        }
        anuncio.setAnunciante(usuario);
        anuncio.setImovel(imovelService.obterPorCodigo(codImovel));
        anuncio.setDatadaPublicacao(new Date());
        anuncioService.salvarAnuncio(anuncio);

        return "redirect:/meus-anuncios";
    }

    @GetMapping(path = "/meus-anuncios")
    public String anunciosDoUsuario(Model model, HttpSession session, @PageableDefault(value=10, page=0) Pageable pageable) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if (usuario == null) {
            model.addAttribute("error", "Sua sessão expirou.");
            return "redirect:/login";
        }

        Page<Anuncio> anuncios = anuncioService.getAnunciosDoUsuario(usuario, pageable);
        model.addAttribute("anuncios", anuncios);

        if (anuncios.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(0, anuncios.getTotalPages() - 1)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        return "anunciosUsuario";
    }

    @GetMapping(path = "/anuncio/{anuncio}/editar")
    public String editarAnuncio(Model model, HttpSession session, @PathVariable(value = "anuncio") String cod) throws AnuncioException.NaoEmcontrado {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if (usuario == null) {
            model.addAttribute("error", "Sua sessão expirou.");
            return "redirect:/login";
        }
        Anuncio anuncio = anuncioService.getById(cod);
        model.addAttribute("anuncio", anuncio);
        System.out.println(anuncio);
        model.addAttribute("tipos", Anuncio.Tipo.values());
        return "novoAnuncio";
    }

    @GetMapping(path = "/meus-anuncios/deletar")
    public String deletarAnuncio(HttpSession session, Model model, @RequestParam(name = "id") Integer id) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if(usuario == null) {
            model.addAttribute("error", "Sua sessão expirou.");
            return "redirect:/login";
        }

        anuncioService.deletar(id);

        return "redirect:/meus-anuncios";
    }

    @GetMapping(path = "/anuncio/{anuncio}")
    public String paginaAnuncio(@PathVariable(value = "anuncio") String id, Model model) throws AnuncioException.NaoEmcontrado {
        model.addAttribute("anuncio", this.anuncioService.getById(id));
        return "anuncio";
    }

    @GetMapping(path = "/busca")
    public String buscarAnuncio(@ModelAttribute BuscaData data, Model model, @PageableDefault(value=20, page=0) Pageable pageable) {
        model.addAttribute("busca", data);
        Page<Anuncio> anuncios = anuncioService.buscarAnuncio(data, pageable);

        model.addAttribute("anuncios", anuncios);

        if (anuncios.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(0, anuncios.getTotalPages() - 1)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        return "resultadoBusca";
    }

    @Transactional
    @GetMapping(path = "/relatorios")
    public String relatorios(Model model, HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if (usuario == null) {
            model.addAttribute("error", "Sua sessão expirou.");
            return "redirect:/login";
        }
        Usuario u = entityManager.merge(usuario);

        model.addAttribute("relatorioDeVendas", u.getRelatorioDeVendas());
        model.addAttribute("relatorioDeAlugueis", u.getRelatorioDeAlugueis());
        return "relatorio";
    }

    @Transactional
    @ResponseBody
    @GetMapping(path = "/relatorios/exportar")
    public String exportarRelatorio(Model model, HttpSession session, HttpServletResponse response,
                                    @RequestParam(value = "formato") Exportar.Formato formato,
                                    @RequestParam(value = "tipo") Anuncio.Tipo tipo,
                                    @RequestParam(value = "template") List<Exportar.Componente> template
                                    ) throws IOException {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogado");
        if (usuario == null) {
            model.addAttribute("error", "Sua sessão expirou.");
            return "redirect:/login";
        }
        Usuario u = entityManager.merge(usuario);

        Relatorio relatorio = null;

        switch (tipo) {
            case ALUGAR:
                relatorio = u.getRelatorioDeAlugueis();
                break;
            case VENDER:
                relatorio = u.getRelatorioDeVendas();
                break;
            default:
                return "Error: informe o 'tipo' de anuncios";
        }

        byte[] bytes = (byte[]) relatorio.exportar().para(formato, template);

        if (!formato.forDownload()) {
            response.setContentType("application/force-download;charset=UTF-8");
            response.setHeader("Content-Transfer-Encoding", "binary");
            response.setHeader("Content-Length", String.valueOf(bytes.length));
            response.setHeader("Content-Disposition", "attachment; filename="+tipo+new Date().getTime()+"."+formato);

            response.getOutputStream().write(bytes);
            return "redirect:/relatorios";
        } else {
            response.setContentType("text/plain;charset=UTF-8");
            response.getOutputStream().write(bytes);
        }

        return null;
    }
}
