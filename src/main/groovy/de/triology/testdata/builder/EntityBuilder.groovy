/**
 * MIT License
 *
 * Copyright (c) 2016 TRIOLOGY GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.triology.testdata.builder

class EntityBuilder {

    //initialized from constructor
    private Map<String, ?> masterEntities
    private Map<String, ?> transactionEntities
    private EntitiesScriptExecutor executor
    private EntityType entityType;

    protected EntityBuilder(final EntitiesScriptExecutor executor, Map<String, ?> masterEntities,
                            Map<String, ?> transactionEntities, EntityType entityType) {
        this.executor = executor
        this.masterEntities = masterEntities
        this.transactionEntities = transactionEntities
        this.entityType = entityType;
    }

    /**
     * Creates an Instance of the specified  entityClass, registers it under the specified entityName and applies the
     * specified entityData definition
     *
     * @param entityClass - the type defining the entity
     * @param entityName - the name to reference the entity e.g. in another call to create
     * @param entityData - a Closure used to build the entity
     * @return the created entity
     */
    public <T> T create(@DelegatesTo.Target Class<T> entityClass, String entityName,
                        @DelegatesTo(strategy = Closure.DELEGATE_FIRST, genericTypeIndex = 0) Closure entityData = {}) {

        T entity = createEntityInstance(entityName, entityClass)

        def rehydrated = entityData.rehydrate(entity, this, this)
        rehydrated.resolveStrategy = Closure.DELEGATE_FIRST
        rehydrated.call()

        executor.fireEntityCreated(entityName, entity)
        return entity
    }

    private <T> T createEntityInstance(final String entityName, final Class<T> entityClass) {
        if ((masterEntities!=null && masterEntities[entityName]) || (transactionEntities!=null && transactionEntities[entityName]!=null)) {
            throw new EntityBuilderException(
            "attempt to create an instance of $entityClass under the name of '$entityName' but an " +
            "entity with that name already exists: ${masterEntities[entityName]}")
        }

        final T entity = entityClass.newInstance()
        if(entityType.equals(EntityType.MASTER)) {
            masterEntities[entityName] = entity
        }else if(entityType.equals(EntityType.TRANSACTION)){
            transactionEntities[entityName] = entity
        }
        return entity
    }

    /**
     * Implementation of Groovy's {@code propertyMissing} that returns the entity previously created under the property
     * name. This Method will be called during entity creation, when an entity is referenced.
     *
     * @param name String
     * @return a previously created entity
     */
    private def propertyMissing(final String name) {
        if (masterEntities!=null && masterEntities[name]) {
            return masterEntities[name]
        }else if(transactionEntities!=null && transactionEntities[name]){
            return transactionEntities[name]
        }
        throw new EntityBuilderException("requested reference for entity with name '$name' cannot be resolved")
    }
}
