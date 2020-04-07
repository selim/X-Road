/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.openapi;

import ee.ria.xroad.common.identifier.XRoadId;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.converter.EndpointConverter;
import org.niis.xroad.restapi.converter.ServiceClientConverter;
import org.niis.xroad.restapi.converter.SubjectConverter;
import org.niis.xroad.restapi.converter.SubjectHelper;
import org.niis.xroad.restapi.dto.AccessRightHolderDto;
import org.niis.xroad.restapi.openapi.model.Endpoint;
import org.niis.xroad.restapi.openapi.model.ServiceClient;
import org.niis.xroad.restapi.openapi.model.Subjects;
import org.niis.xroad.restapi.service.AccessRightService;
import org.niis.xroad.restapi.service.ClientNotFoundException;
import org.niis.xroad.restapi.service.EndpointNotFoundException;
import org.niis.xroad.restapi.service.EndpointService;
import org.niis.xroad.restapi.service.IdentifierNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.niis.xroad.restapi.util.FormatUtils.parseLongIdOrThrowNotFound;

/**
 * Endpoints api
 */
@Controller
@RequestMapping("/api")
@Slf4j
@PreAuthorize("denyAll")
public class EndpointsApiController implements EndpointsApi {

    private final EndpointService endpointService;
    private final EndpointConverter endpointConverter;
    private final AccessRightService accessRightService;
    private final ServiceClientConverter serviceClientConverter;
    private final SubjectConverter subjectConverter;
    private final SubjectHelper subjectHelper;

    private static final String NOT_FOUND_ERROR_MSG = "Endpoint not found with id";


    @Autowired
    public EndpointsApiController(
            EndpointService endpointService,
            EndpointConverter endpointConverter,
            AccessRightService accessRightService,
            ServiceClientConverter serviceClientConverter,
            SubjectConverter subjectConverter,
            SubjectHelper subjectHelper) {
        this.endpointService = endpointService;
        this.endpointConverter = endpointConverter;
        this.accessRightService = accessRightService;
        this.serviceClientConverter = serviceClientConverter;
        this.subjectConverter = subjectConverter;
        this.subjectHelper = subjectHelper;
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_ENDPOINT')")
    public ResponseEntity<Endpoint> getEndpoint(String id) {
        Long endpointId = parseLongIdOrThrowNotFound(id);
        Endpoint endpoint;
        try {
            endpoint = endpointConverter.convert(endpointService.getEndpoint(endpointId));
        } catch (EndpointNotFoundException e) {
            throw new ResourceNotFoundException(NOT_FOUND_ERROR_MSG + " " + id);
        }
        return new ResponseEntity(endpoint, HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('DELETE_ENDPOINT')")
    public ResponseEntity<Void> deleteEndpoint(String id) {
        Long endpointId = parseLongIdOrThrowNotFound(id);
        try {
            endpointService.deleteEndpoint(endpointId);
        } catch (EndpointNotFoundException e) {
            throw new ResourceNotFoundException(e);
        } catch (ClientNotFoundException e) {
            throw new ConflictException("Client not found for the given endpoint with id: " + id);
        } catch (EndpointService.IllegalGeneratedEndpointRemoveException e) {
            throw new BadRequestException("Removing is not allowed for generated endpoint " + id);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_OPENAPI3_ENDPOINT')")
    public ResponseEntity<Endpoint> updateEndpoint(String id, Endpoint endpoint) {
        Long endpointId = parseLongIdOrThrowNotFound(id);
        Endpoint ep;
        try {
            ep = endpointConverter.convert(endpointService.updateEndpoint(endpointId, endpoint));
        } catch (EndpointNotFoundException e) {
            throw new ResourceNotFoundException(e);
        } catch (EndpointService.IllegalGeneratedEndpointUpdateException e) {
            throw new BadRequestException("Updating is not allowed for generated endpoint " + id);
        }

        return new ResponseEntity<>(ep, HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('VIEW_ENDPOINT_ACL')")
    public ResponseEntity<List<ServiceClient>> getEndpointAccessRights(String id) {
        Long endpointId = parseLongIdOrThrowNotFound(id);
        List<AccessRightHolderDto> accessRightHoldersByEndpoint;
        try {
            accessRightHoldersByEndpoint = accessRightService.getAccessRightHoldersByEndpoint(endpointId);
        } catch (EndpointNotFoundException e) {
            throw new ResourceNotFoundException(NOT_FOUND_ERROR_MSG + " " + id);
        } catch (ClientNotFoundException e) {
            throw new ConflictException(e);
        }
        List<ServiceClient> serviceClients = serviceClientConverter
                .convertAccessRightHolderDtos(accessRightHoldersByEndpoint);
        return new ResponseEntity<>(serviceClients, HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_ENDPOINT_ACL')")
    public ResponseEntity<List<ServiceClient>> addEndpointAccessRights(String id, Subjects subjects) {
        Long endpointId = parseLongIdOrThrowNotFound(id);
        Set<Long> localGroupIds = subjectHelper.getLocalGroupIds(subjects);
        List<XRoadId> xRoadIds = subjectHelper.getXRoadIdsButSkipLocalGroups(subjects);
        List<AccessRightHolderDto> accessRightHoldersByEndpoint = null;

        try {
            accessRightHoldersByEndpoint = accessRightService.addEndpointAccessRights(endpointId,
                    new HashSet<>(xRoadIds), localGroupIds);
        } catch (EndpointNotFoundException | AccessRightService.AccessRightNotFoundException e) {
            throw new ResourceNotFoundException(e);
        } catch (ClientNotFoundException | AccessRightService.DuplicateAccessRightException  e) {
            throw new ConflictException(e);
        } catch (IdentifierNotFoundException e) {
            throw new BadRequestException(e);
        }

        List<ServiceClient> serviceClients = serviceClientConverter
                .convertAccessRightHolderDtos(accessRightHoldersByEndpoint);
        return new ResponseEntity<>(serviceClients, HttpStatus.CREATED);
    }

    @Override
    @PreAuthorize("hasAuthority('EDIT_ENDPOINT_ACL')")
    public ResponseEntity<Void> deleteEndpointAccessRights(String id, Subjects subjects) {
        Long endpointId = parseLongIdOrThrowNotFound(id);
        Set<Long> localGroupIds = subjectHelper.getLocalGroupIds(subjects);
        HashSet<XRoadId> xRoadIds = new HashSet<>(subjectHelper.getXRoadIdsButSkipLocalGroups(subjects));
        try {
            accessRightService.deleteEndpointAccessRights(endpointId, xRoadIds, localGroupIds);
        } catch (EndpointNotFoundException | AccessRightService.AccessRightNotFoundException e) {
            throw new ResourceNotFoundException(e);
        } catch (ClientNotFoundException e) {
            throw new ConflictException(e);
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }


}
