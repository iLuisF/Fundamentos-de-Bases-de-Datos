/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Controladores;

import Controladores.exceptions.NonexistentEntityException;
import Controladores.exceptions.PreexistingEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import Entidades.Agentes;
import Entidades.Automoviles;
import Entidades.Multas;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Table;

/**
 *
 * @author Luis
 */
@Table(name = "MULTAS", schema = "Agencia")
@ManagedBean
@RequestScoped
public class MultasJpaController implements Serializable {

    public MultasJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }

    private EntityManagerFactory emf = null;

    public MultasJpaController() {
        this.emf = Persistence.createEntityManagerFactory("BD-Practica12-V2PU");
    }

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Multas multas) throws PreexistingEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Agentes numPlaca = multas.getNumPlaca();
            if (numPlaca != null) {
                numPlaca = em.getReference(numPlaca.getClass(), numPlaca.getNumPlaca());
                multas.setNumPlaca(numPlaca);
            }
            Automoviles numMotor = multas.getNumMotor();
            if (numMotor != null) {
                numMotor = em.getReference(numMotor.getClass(), numMotor.getNumMotor());
                multas.setNumMotor(numMotor);
            }
            em.persist(multas);
            if (numPlaca != null) {
                numPlaca.getMultasList().add(multas);
                numPlaca = em.merge(numPlaca);
            }
            if (numMotor != null) {
                numMotor.getMultasList().add(multas);
                numMotor = em.merge(numMotor);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findMultas(multas.getIdMulta()) != null) {
                throw new PreexistingEntityException("Multas " + multas + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Multas multas) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Multas persistentMultas = em.find(Multas.class, multas.getIdMulta());
            Agentes numPlacaOld = persistentMultas.getNumPlaca();
            Agentes numPlacaNew = multas.getNumPlaca();
            Automoviles numMotorOld = persistentMultas.getNumMotor();
            Automoviles numMotorNew = multas.getNumMotor();
            if (numPlacaNew != null) {
                numPlacaNew = em.getReference(numPlacaNew.getClass(), numPlacaNew.getNumPlaca());
                multas.setNumPlaca(numPlacaNew);
            }
            if (numMotorNew != null) {
                numMotorNew = em.getReference(numMotorNew.getClass(), numMotorNew.getNumMotor());
                multas.setNumMotor(numMotorNew);
            }
            multas = em.merge(multas);
            if (numPlacaOld != null && !numPlacaOld.equals(numPlacaNew)) {
                numPlacaOld.getMultasList().remove(multas);
                numPlacaOld = em.merge(numPlacaOld);
            }
            if (numPlacaNew != null && !numPlacaNew.equals(numPlacaOld)) {
                numPlacaNew.getMultasList().add(multas);
                numPlacaNew = em.merge(numPlacaNew);
            }
            if (numMotorOld != null && !numMotorOld.equals(numMotorNew)) {
                numMotorOld.getMultasList().remove(multas);
                numMotorOld = em.merge(numMotorOld);
            }
            if (numMotorNew != null && !numMotorNew.equals(numMotorOld)) {
                numMotorNew.getMultasList().add(multas);
                numMotorNew = em.merge(numMotorNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = multas.getIdMulta();
                if (findMultas(id) == null) {
                    throw new NonexistentEntityException("The multas with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Multas multas;
            try {
                multas = em.getReference(Multas.class, id);
                multas.getIdMulta();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The multas with id " + id + " no longer exists.", enfe);
            }
            Agentes numPlaca = multas.getNumPlaca();
            if (numPlaca != null) {
                numPlaca.getMultasList().remove(multas);
                numPlaca = em.merge(numPlaca);
            }
            Automoviles numMotor = multas.getNumMotor();
            if (numMotor != null) {
                numMotor.getMultasList().remove(multas);
                numMotor = em.merge(numMotor);
            }
            em.remove(multas);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Multas> findMultasEntities() {
        return findMultasEntities(true, -1, -1);
    }

    public List<Multas> findMultasEntities(int maxResults, int firstResult) {
        return findMultasEntities(false, maxResults, firstResult);
    }

    private List<Multas> findMultasEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Multas.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Multas findMultas(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Multas.class, id);
        } finally {
            em.close();
        }
    }

    public int getMultasCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Multas> rt = cq.from(Multas.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }

    public List<Multas> getMultasSinSeguro() {
        EntityManager em = getEntityManager();
        List<Object[]> results;
        results = em.createNativeQuery("SELECT mul.id_multa, mul.num_placa, mul.num_motor, mul.monto, mul.infraccion\n"
                + "FROM APP.AGENCIA.MULTAS mul INNER JOIN\n"
                + "	(SELECT  *\n"
                + "FROM APP.AGENCIA.TAXIS \n"
                + "WHERE id_aseguradora is null) tax ON mul.num_motor = tax.num_motor").getResultList();
        List<Multas> multas = new LinkedList<>();
        for (int i = 0; i < results.size(); i++) {
            Multas nueva = new Multas();
            nueva.setIdMulta((Integer) results.get(i)[0]);
            nueva.setNumPlaca(new Agentes((String) results.get(i)[1]));
            nueva.setNumMotor(new Automoviles((String) results.get(i)[2]));
            nueva.setMonto(new BigInteger(results.get(i)[3].toString()));
            nueva.setInfraccion((String) results.get(i)[4]);
            nueva.setHora(null);
            nueva.setEstado(null);
            nueva.setDelegacionMunicipio(null);
            nueva.setColonia(null);
            nueva.setCiudad(null);
            nueva.setCalle(null);
            nueva.setFecha(null);
            multas.add(nueva);
        }
        return multas;
    }
}
