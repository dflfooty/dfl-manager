package net.dflmngr.model.dao.impl;

//import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import net.dflmngr.model.dao.GenericDao;

public abstract class GenericDaoImpl<E, K> implements GenericDao<E, K> {
	
	protected Class<E> entityClass;
	protected CriteriaBuilder criteriaBuilder;
	protected CriteriaQuery<E> criteriaQuery;
	protected CriteriaDelete<E> criteriaDelete;
	protected TypedQuery<E> query;
	protected Root<E> entity;
	private static EntityManagerFactory factory;
	
	@PersistenceContext
	protected EntityManager entityManager;
	
	public GenericDaoImpl(Class<E> entityClass) {
		//ParameterizedType genericSuperclass = (ParameterizedType) getClass().getGenericSuperclass();
		//this.entityClass = (Class<E>) genericSuperclass.getActualTypeArguments()[0];
		this.entityClass = entityClass;

		String url = System.getenv("JDBC_DATABASE_URL");

		Map<String, Object> configOverrides = new HashMap<>();
		configOverrides.put("javax.persistence.jdbc.url", url);

		Map<String, String> env = System.getenv();
		for (String envName : env.keySet()) {
			if(envName.contains("javax.persistence")) {
				configOverrides.put(envName, env.get(envName));
			} else if(envName.contains("eclipselink")) {
				configOverrides.put(envName, env.get(envName));
			}
		}
		
		factory = Persistence.createEntityManagerFactory("dflmngr", configOverrides);
		entityManager = factory.createEntityManager();
	}
	
	public void persist(E entity) {
		entityManager.persist(entity);
	}
	
	public void merge(E entity) {
		entityManager.detach(entity);
		entityManager.merge(entity);
	}
	
	public void remove(E entity) {
		entityManager.remove(entity);
	}
	
	public E findById(K id) {
		E entity = entityManager.find(entityClass, id);
		return entity;
	}
	
	public List<E> findAll() {		
		criteriaBuilder = entityManager.getCriteriaBuilder();
		criteriaQuery = criteriaBuilder.createQuery(entityClass);
		criteriaQuery.from(entityClass);
		List<E> entitys = entityManager.createQuery(criteriaQuery).getResultList();
		return entitys;
	}
	
	public void commit() {
		entityManager.getTransaction().commit();
	}
	
	public void beginTransaction() {
		entityManager.getTransaction().begin();
	}
	
	public void flush() {
		entityManager.flush();
	}
	
	public void refresh(E entity) {
		entityManager.refresh(entity);
	}
	
	public void close() {
		entityManager.close();
	}
}
