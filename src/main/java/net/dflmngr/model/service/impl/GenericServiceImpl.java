package net.dflmngr.model.service.impl;

import java.util.List;

import net.dflmngr.model.dao.GenericDao;
import net.dflmngr.model.service.GenericService;

public class GenericServiceImpl<E, K> implements GenericService<E, K>  {
	
	private GenericDao<E, K> genericDao;
	
    protected void setDao(GenericDao<E, K> dao) {
        this.genericDao = dao;
    }
    
    public E get(K id) {
    	return genericDao.findById(id);
    }
	
	public List<E> findAll() {
		return genericDao.findAll();
	}
	
	public void insert(E entity) {
		genericDao.beginTransaction();
		genericDao.persist(entity);
		genericDao.commit();
	}
	
	public void update(E entity) {
		genericDao.merge(entity);
	}
	
	public void insertAll(List<E> entitys, boolean inTx) {
		
		if(!inTx) {
			genericDao.beginTransaction();
		}
		
		for(E e : entitys) {
			genericDao.persist(e);
		}
		
		if(!inTx) {
			genericDao.commit();
		}
	}
	
	public void updateAll(List<E> entitys, boolean inTx) {
		
		if(!inTx) {
			genericDao.beginTransaction();
		}
		
		for(E e : entitys) {
			genericDao.merge(e);
		}
		
		if(!inTx) {
			genericDao.commit();
		}
	}
	
	public void delete(E entity) {
		genericDao.remove(entity);
	}
	
	public void replaceAll(List<E> entitys) {
		genericDao.beginTransaction();
		List<E> existingEntitys = findAll();
		for(E entity : existingEntitys) {
			genericDao.remove(entity);
		}
		
		genericDao.flush();
		
		for(E entity : entitys) {
			genericDao.persist(entity);
		}
		genericDao.commit();
	}
	
	public void refresh(E entity) {
		genericDao.refresh(entity);
	}
	
	public void close() {
		genericDao.close();
	}
}
