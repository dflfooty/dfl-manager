package net.dflmngr.model.service.impl;

import java.util.List;

import net.dflmngr.model.dao.GenericDao;
import net.dflmngr.model.service.GenericService;

public class GenericServiceImpl<E, K> implements GenericService<E, K>  {
	
	public GenericDao<E, K> dao;
	
    protected void setDao(GenericDao<E, K> dao) {
        this.dao = dao;
    }
    
    public E get(K id) {
    	E entity = dao.findById(id);
    	return entity;
    }
	
	public List<E> findAll() {
		return dao.findAll();
	}
	
	public void insert(E entity) {
		dao.beginTransaction();
		dao.persist(entity);
		dao.commit();
	}
	
	public void update(E entity) {
		dao.merge(entity);
	}
	
	public void insertAll(List<E> entitys, boolean inTx) {
		
		if(!inTx) {
			dao.beginTransaction();
		}
		
		for(E e : entitys) {
			dao.persist(e);
		}
		
		if(!inTx) {
			dao.commit();
		}
	}
	
	public void updateAll(List<E> entitys, boolean inTx) {
		
		if(!inTx) {
			dao.beginTransaction();
		}
		
		for(E e : entitys) {
			dao.merge(e);
		}
		
		if(!inTx) {
			dao.commit();
		}
	}
	
	public void delete(E entity) {
		dao.remove(entity);
	}
	
	public void replaceAll(List<E> entitys) {
		dao.beginTransaction();
		List<E> existingEntitys = findAll();
		for(E entity : existingEntitys) {
			dao.remove(entity);
		}
		
		dao.flush();
		
		for(E entity : entitys) {
			dao.persist(entity);
		}
		dao.commit();
	}
	
	public void refresh(E entity) {
		dao.refresh(entity);
	}
	
	public void close() {
		dao.close();
	}
}
