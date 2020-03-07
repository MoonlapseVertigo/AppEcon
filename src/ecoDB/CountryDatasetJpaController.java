/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ecoDB;

import ecoDB.exceptions.NonexistentEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author moonlapsevertigo
 */
public class CountryDatasetJpaController implements Serializable {

    public CountryDatasetJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(CountryDataset countryDataset) {
        if (countryDataset.getCountryDataCollection() == null) {
            countryDataset.setCountryDataCollection(new ArrayList<CountryData>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Country countryCode = countryDataset.getCountryCode();
            if (countryCode != null) {
                countryCode = em.getReference(countryCode.getClass(), countryCode.getIsoCode());
                countryDataset.setCountryCode(countryCode);
            }
            Collection<CountryData> attachedCountryDataCollection = new ArrayList<CountryData>();
            for (CountryData countryDataCollectionCountryDataToAttach : countryDataset.getCountryDataCollection()) {
                countryDataCollectionCountryDataToAttach = em.getReference(countryDataCollectionCountryDataToAttach.getClass(), countryDataCollectionCountryDataToAttach.getId());
                attachedCountryDataCollection.add(countryDataCollectionCountryDataToAttach);
            }
            countryDataset.setCountryDataCollection(attachedCountryDataCollection);
            em.persist(countryDataset);
            if (countryCode != null) {
                countryCode.getCountryDatasetCollection().add(countryDataset);
                countryCode = em.merge(countryCode);
            }
            for (CountryData countryDataCollectionCountryData : countryDataset.getCountryDataCollection()) {
                CountryDataset oldDatasetOfCountryDataCollectionCountryData = countryDataCollectionCountryData.getDataset();
                countryDataCollectionCountryData.setDataset(countryDataset);
                countryDataCollectionCountryData = em.merge(countryDataCollectionCountryData);
                if (oldDatasetOfCountryDataCollectionCountryData != null) {
                    oldDatasetOfCountryDataCollectionCountryData.getCountryDataCollection().remove(countryDataCollectionCountryData);
                    oldDatasetOfCountryDataCollectionCountryData = em.merge(oldDatasetOfCountryDataCollectionCountryData);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(CountryDataset countryDataset) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            CountryDataset persistentCountryDataset = em.find(CountryDataset.class, countryDataset.getDatasetId());
            Country countryCodeOld = persistentCountryDataset.getCountryCode();
            Country countryCodeNew = countryDataset.getCountryCode();
            Collection<CountryData> countryDataCollectionOld = persistentCountryDataset.getCountryDataCollection();
            Collection<CountryData> countryDataCollectionNew = countryDataset.getCountryDataCollection();
            if (countryCodeNew != null) {
                countryCodeNew = em.getReference(countryCodeNew.getClass(), countryCodeNew.getIsoCode());
                countryDataset.setCountryCode(countryCodeNew);
            }
            Collection<CountryData> attachedCountryDataCollectionNew = new ArrayList<CountryData>();
            for (CountryData countryDataCollectionNewCountryDataToAttach : countryDataCollectionNew) {
                countryDataCollectionNewCountryDataToAttach = em.getReference(countryDataCollectionNewCountryDataToAttach.getClass(), countryDataCollectionNewCountryDataToAttach.getId());
                attachedCountryDataCollectionNew.add(countryDataCollectionNewCountryDataToAttach);
            }
            countryDataCollectionNew = attachedCountryDataCollectionNew;
            countryDataset.setCountryDataCollection(countryDataCollectionNew);
            countryDataset = em.merge(countryDataset);
            if (countryCodeOld != null && !countryCodeOld.equals(countryCodeNew)) {
                countryCodeOld.getCountryDatasetCollection().remove(countryDataset);
                countryCodeOld = em.merge(countryCodeOld);
            }
            if (countryCodeNew != null && !countryCodeNew.equals(countryCodeOld)) {
                countryCodeNew.getCountryDatasetCollection().add(countryDataset);
                countryCodeNew = em.merge(countryCodeNew);
            }
            for (CountryData countryDataCollectionOldCountryData : countryDataCollectionOld) {
                if (!countryDataCollectionNew.contains(countryDataCollectionOldCountryData)) {
                    countryDataCollectionOldCountryData.setDataset(null);
                    countryDataCollectionOldCountryData = em.merge(countryDataCollectionOldCountryData);
                }
            }
            for (CountryData countryDataCollectionNewCountryData : countryDataCollectionNew) {
                if (!countryDataCollectionOld.contains(countryDataCollectionNewCountryData)) {
                    CountryDataset oldDatasetOfCountryDataCollectionNewCountryData = countryDataCollectionNewCountryData.getDataset();
                    countryDataCollectionNewCountryData.setDataset(countryDataset);
                    countryDataCollectionNewCountryData = em.merge(countryDataCollectionNewCountryData);
                    if (oldDatasetOfCountryDataCollectionNewCountryData != null && !oldDatasetOfCountryDataCollectionNewCountryData.equals(countryDataset)) {
                        oldDatasetOfCountryDataCollectionNewCountryData.getCountryDataCollection().remove(countryDataCollectionNewCountryData);
                        oldDatasetOfCountryDataCollectionNewCountryData = em.merge(oldDatasetOfCountryDataCollectionNewCountryData);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = countryDataset.getDatasetId();
                if (findCountryDataset(id) == null) {
                    throw new NonexistentEntityException("The countryDataset with id " + id + " no longer exists.");
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
            CountryDataset countryDataset;
            try {
                countryDataset = em.getReference(CountryDataset.class, id);
                countryDataset.getDatasetId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The countryDataset with id " + id + " no longer exists.", enfe);
            }
            Country countryCode = countryDataset.getCountryCode();
            if (countryCode != null) {
                countryCode.getCountryDatasetCollection().remove(countryDataset);
                countryCode = em.merge(countryCode);
            }
            Collection<CountryData> countryDataCollection = countryDataset.getCountryDataCollection();
            for (CountryData countryDataCollectionCountryData : countryDataCollection) {
                countryDataCollectionCountryData.setDataset(null);
                countryDataCollectionCountryData = em.merge(countryDataCollectionCountryData);
            }
            em.remove(countryDataset);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<CountryDataset> findCountryDatasetEntities() {
        return findCountryDatasetEntities(true, -1, -1);
    }

    public List<CountryDataset> findCountryDatasetEntities(int maxResults, int firstResult) {
        return findCountryDatasetEntities(false, maxResults, firstResult);
    }

    private List<CountryDataset> findCountryDatasetEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(CountryDataset.class));
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

    public CountryDataset findCountryDataset(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(CountryDataset.class, id);
        } finally {
            em.close();
        }
    }

    public int getCountryDatasetCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<CountryDataset> rt = cq.from(CountryDataset.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
