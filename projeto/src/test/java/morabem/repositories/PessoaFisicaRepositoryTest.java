package morabem.repositories;

import builders.EnderecoBuilder;

import builders.ImovelBuilder;
import builders.UsuarioBuilder;
import morabem.domain.Endereco;
import morabem.domain.Imovel;
import morabem.domain.PessoaFisica;
import morabem.services.UsuarioService;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;


@RunWith(SpringRunner.class)
@DataJpaTest
public class PessoaFisicaRepositoryTest {

    @Autowired
    public TestEntityManager testEntityManager;

    @Autowired
    public PessoaFisicaRepository pessoaFisicaRepository;

    @Autowired
    public EnderecoRepository enderecoRepository;

    @Autowired
    public ImovelRepository imovelRepository;


    @Test
    public void salvarPessoaFisica() {
        Endereco end = EnderecoBuilder.obterUm().agora();
        PessoaFisica usuario = UsuarioBuilder.PessoaFisica.obterUm()
                .comASenha("123456").comOEmail("user@email").comEndereco(end).agora();
        enderecoRepository.save(end);
        enderecoRepository.flush();
        pessoaFisicaRepository.save(usuario);
        pessoaFisicaRepository.flush();
    }
    

    @Test
    public void cadastrarDiferente() {
    	PessoaFisica pessoafisica = UsuarioBuilder.PessoaFisica.obterUm().agora();
    	
    	enderecoRepository.saveAndFlush(pessoafisica.getEndereco());
        PessoaFisica pessoafisicaSalva = pessoaFisicaRepository.saveAndFlush(pessoafisica);
        
        assertThat(pessoafisica, is(equalTo(pessoafisicaSalva)));
        assertThat(pessoafisica.getId(), is(notNullValue()));
        
    }
    
    @Test
    public void alterar() {
    	PessoaFisica pessoafisica = pessoaFisicaRepository.findById(1L).get();

        pessoafisica.setNome("Novo nome");
        pessoaFisicaRepository.saveAndFlush(pessoafisica);
        assertThat(pessoafisica.getNome(), is(equalTo("Novo nome")));
    	
    }
}