/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest;

import com.ericsson.adp.mgmt.backupandrestore.agent.Agent;
import com.ericsson.adp.mgmt.backupandrestore.agent.AgentRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.exception.AnotherActionRunningException;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.exception.NotImplementedException;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnprocessableEntityException;
import com.ericsson.adp.mgmt.backupandrestore.rest.error.ErrorResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base Rest Controller, encapsulates common behavior, such as logging and error handling.
 */
public abstract class BaseController {

    private static final String SCOPE_SEPARATOR = ";";
    private static final Logger logger = LogManager.getLogger(BaseController.class);
    private static final String HTTP_STATUS_ERROR_LOG_MESSAGE = "Returning {}";

    @Autowired
    protected BackupManagerRepository backupManagerRepository;

    @Autowired
    protected AgentRepository agentRepository;

    /**
     * Executed when a RuntimeException is thrown.
     * @param exception runtimeException
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(final RuntimeException exception) {
        logger.error(HTTP_STATUS_ERROR_LOG_MESSAGE, HttpStatus.INTERNAL_SERVER_ERROR, exception);
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage());
    }

    /**
     * Executed when a NotFoundException is thrown.
     * @param exception notFoundException
     * @return 404 Not Found
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFoundException(final NotFoundException exception) {
        logger.error(HTTP_STATUS_ERROR_LOG_MESSAGE, HttpStatus.NOT_FOUND, exception);
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
    }

    /**
     * Executed when a NotImplementedException is thrown.
     * @param exception NotImplementedException
     * @return 501 Not Implemented
     */
    @ExceptionHandler(NotImplementedException.class)
    @ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED)
    public ErrorResponse handleNotImplementedException(final NotImplementedException exception) {
        return new ErrorResponse(HttpStatus.NOT_IMPLEMENTED.value(), exception.getMessage());
    }

    /**
     * Executed when a HttpMessageNotReadableException is thrown.
     * @param exception httpMessageNotReadableException
     * @return 400 Bad Request Code
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBackupServiceException(final HttpMessageNotReadableException exception) {
        logger.error(HTTP_STATUS_ERROR_LOG_MESSAGE, HttpStatus.BAD_REQUEST, exception);
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
    }

    /**
     * Executed when a UnprocessableEntityException is thrown.
     * @param exception UnprocessableEntityException
     * @return 422 UnprocessableEntity
     */
    @ExceptionHandler(UnprocessableEntityException.class)
    @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleUnprocessableEntityException(final UnprocessableEntityException exception) {
        logger.error(HTTP_STATUS_ERROR_LOG_MESSAGE, HttpStatus.UNPROCESSABLE_ENTITY, exception);
        return new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage());
    }

    /**
     * Handle a MethodArgumentNotValidException
     * @param exception MethodArgumentNotValidException
     * @return 400 BAD_REQUEST ErrorResponse
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleRequestBodyValidationException(final MethodArgumentNotValidException exception) {
        logger.error(HTTP_STATUS_ERROR_LOG_MESSAGE, HttpStatus.BAD_REQUEST, exception);
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getBindingResult().getAllErrors().get(0).getDefaultMessage());
    }

    /**
     * Handle an AnotherActionRunningException
     * @param exception AnotherActionRunningException
     * @return 409 CONFLICT ErrorResponse
     */
    @ExceptionHandler(AnotherActionRunningException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public ErrorResponse handleAnotherActionRunningException(final AnotherActionRunningException exception) {
        logger.error(HTTP_STATUS_ERROR_LOG_MESSAGE, HttpStatus.CONFLICT, exception);
        return new ErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage());
    }

    /**
     * Get one backupManager.
     * @param backupManagerId to look for.
     * @return backupManager.
     */
    protected BackupManager getBackupManager(final String backupManagerId) {
        return backupManagerRepository.getBackupManager(backupManagerId);
    }

    /**
     * Get agents registered to a backup manager.
     * @param backupManager the backup manager
     * @return list of agent ids registered to a backup manager.
     */
    protected List<String> getAgents(final BackupManager backupManager) {
        return agentRepository.getAgents().stream()
                .filter(agent -> Arrays.asList(agent.getScope().split(SCOPE_SEPARATOR)).contains(backupManager.getBackupManagerId()))
                .map(Agent::getAgentId)
                .collect(Collectors.toList());
    }
}
