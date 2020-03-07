/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ecoDB;

import ecoDB.exceptions.NonexistentEntityException;
import ecoDB.exceptions.PreexistingEntityException;
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
public class CountryJpaController implements Serializable {

    public CountryJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Country country) throws PreexistingEntityException, Exception {
        if (country.getCountryDatasetCollection() == null) {
            country.setCountryDatasetCollection(new ArrayList<CountryDataset>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<CountryDataset> attachedCountryDatasetCollection = new ArrayList<CountryDataset>();
            for (CountryDataset countryDatasetCollectionCountryDatasetToAttach : country.getCountryDatasetCollection()) {
                countryDatasetCollectionCountryDatasetToAttach = em.getReference(countryDatasetCollectionCountryDatasetToAttach.getClass(), countryDatasetCollectionCountryDatasetToAttach.getDatasetId());
                attachedCountryDatasetCollection.add(countryDatasetCollectionCountryDatasetToAttach);
            }
            country.setCountryDatasetCollection(attachedCountryDatasetCollection);
            em.persist(country);
            for (CountryDataset countryDatasetCollectionCountryDataset : country.getCountryDatasetCollection()) {
                Country oldCountryCodeOfCountryDatasetCollectionCountryDataset = countryDatasetCollectionCountryDataset.getCountryCode();
                countryDatasetCollectionCountryDataset.setCountryCode(country);
                countryDatasetCollectionCountryDataset = em.merge(countryDatasetCollectionCountryDataset);
                if (oldCountryCodeOfCountryDatasetCollectionCountryDataset != null) {
                    oldCountryCodeOfCountryDatasetCollectionCountryDataset.getCountryDatasetCollection().remove(countryDatasetCollectionCountryDataset);
                    oldCountryCodeOfCountryDatasetCollectionCountryDataset = em.merge(oldCountryCodeOfCountryDatasetCollectionCountryDataset);
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findCountry(country.getIsoCode()) != null) {
                throw new PreexistingEntityException("Country " + country + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Country country) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Country persistentCountry = em.find(Country.class, country.getIsoCode());
            Collection<CountryDataset> countryDatasetCollectionOld = persistentCountry.getCountryDatasetCollection();
            Collection<CountryDataset> countryDatasetCollectionNew = country.getCountryDatasetCollection();
            Collection<CountryDataset> attachedCountryDatasetCollectionNew = new ArrayList<CountryDataset>();
            for (CountryDataset countryDatasetCollectionNewCountryDatasetToAttach : countryDatasetCollectionNew) {
                countryDatasetCollectionNewCountryDatasetToAttach = em.getReference(countryDatasetCollectionNewCountryDatasetToAttach.getClass(), countryDatasetCollectionNewCountryDatasetToAttach.getDatasetId());
                attachedCountryDatasetCollectionNew.add(countryDatasetCollectionNewCountryDatasetToAttach);
            }
            countryDatasetCollectionNew = attachedCountryDatasetCollectionNew;
            country.setCountryDatasetCollection(countryDatasetCollectionNew);
            country = em.merge(country);
            for (CountryDataset countryDatasetCollectionOldCountryDataset : countryDatasetCollectionOld) {
                if (!countryDatasetCollectionNew.contains(countryDatasetCollectionOldCountryDataset)) {
                    countryDatasetCollectionOldCountryDataset.setCountryCode(null);
                    countryDatasetCollectionOldCountryDataset = em.merge(countryDatasetCollectionOldCountryDataset);
                }
            }
            for (CountryDataset countryDatasetCollectionNewCountryDataset : countryDatasetCollectionNew) {
                if (!countryDatasetCollectionOld.contains(countryDatasetCollectionNewCountryDataset)) {
                    Country oldCountryCodeOfCountryDatasetCollectionNewCountryDataset = countryDatasetCollectionNewCountryDataset.getCountryCode();
                    countryDatasetCollectionNewCountryDataset.setCountryCode(country);
                    countryDatasetCollectionNewCountryDataset = em.merge(countryDatasetCollectionNewCountryDataset);
                    if (oldCountryCodeOfCountryDatasetCollectionNewCountryDataset != null && !oldCountryCodeOfCountryDatasetCollectionNewCountryDataset.equals(country)) {
                        oldCountryCodeOfCountryDatasetCollectionNewCountryDataset.getCountryDatasetCollection().remove(countryDatasetCollectionNewCountryDataset);
                        oldCountryCodeOfCountryDatasetCollectionNewCountryDataset = em.merge(oldCountryCodeOfCountryDatasetCollectionNewCountryDataset);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                String id = country.getIsoCode();
                if (findCountry(id) == null) {
                    throw new NonexistentEntityException("The country with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(String id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Country country;
            try {
                country = em.getReference(Country.class, id);
                country.getIsoCode();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The country with id " + id + " no longer exists.", enfe);
            }
            Collection<CountryDataset> countryDatasetCollection = country.getCountryDatasetCollection();
            for (CountryDataset countryDatasetCollectionCountryDataset : countryDatasetCollection) {
                countryDatasetCollectionCountryDataset.setCountryCode(null);
                countryDatasetCollectionCountryDataset = em.merge(countryDatasetCollectionCountryDataset);
            }
            em.remove(country);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Country> findCountryEntities() {
        return findCountryEntities(true, -1, -1);
    }

    public List<Country> findCountryEntities(int maxResults, int firstResult) {
        return findCountryEntities(false, maxResults, firstResult);
    }

    private List<Country> findCountryEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Country.class));
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

    public Country findCountry(String id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Country.class, id);
        } finally {
            em.close();
        }
    }

    public int getCountryCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Country> rt = cq.from(Country.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
