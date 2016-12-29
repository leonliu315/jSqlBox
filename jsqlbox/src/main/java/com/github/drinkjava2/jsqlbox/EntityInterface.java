package com.github.drinkjava2.jsqlbox;

/**
 * Each entity bean class should extends from EntityBase class <br/>
 * But for some reason if don't want extends from EntityBase class, just copy all fields and methods in EntityBase class
 * to your entity bean class
 * 
 */
public interface EntityInterface {

	public default Box box() {
		return Box.getBox(this);
	}

	public default String table() {
		return box().getRealTable();
	}

	public default String star() {
		return box().getStar();
	}

	public default void insert() {
		this.box().insert();
	}

	public default void update() {
		this.box().update();
	}

	public default void delete() {
		this.box().delete();
	}

	public default String fieldID(String realColumnName) {
		return this.box().fieldID(realColumnName);
	}

}