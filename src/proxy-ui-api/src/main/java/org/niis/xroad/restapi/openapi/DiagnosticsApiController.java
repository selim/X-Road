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

import ee.ria.xroad.common.DiagnosticsStatus;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.converter.GlobalConfDiagnosticConverter;
import org.niis.xroad.restapi.converter.OcspResponderDiagnosticConverter;
import org.niis.xroad.restapi.converter.TimestampingServiceDiagnosticConverter;
import org.niis.xroad.restapi.dto.OcspResponderDiagnosticsStatus;
import org.niis.xroad.restapi.openapi.model.GlobalConfDiagnostics;
import org.niis.xroad.restapi.openapi.model.OcspResponderDiagnostics;
import org.niis.xroad.restapi.openapi.model.TimestampingServiceDiagnostics;
import org.niis.xroad.restapi.service.DiagnosticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * diagnostics api
 */
@Controller
@RequestMapping("/api")
@Slf4j
@PreAuthorize("denyAll")
public class DiagnosticsApiController implements DiagnosticsApi {

    private DiagnosticService diagnosticService;
    private GlobalConfDiagnosticConverter globalConfDiagnosticConverter;
    private TimestampingServiceDiagnosticConverter timestampingServiceDiagnosticConverter;
    private OcspResponderDiagnosticConverter ocspResponderDiagnosticConverter;

    /**
     * DiagnosticsApiController constructor
     */
    @Autowired
    public DiagnosticsApiController(DiagnosticService diagnosticService,
            GlobalConfDiagnosticConverter globalConfDiagnosticConverter,
            TimestampingServiceDiagnosticConverter timestampingServiceDiagnosticConverter,
            OcspResponderDiagnosticConverter ocspResponderDiagnosticConverter) {
        this.diagnosticService = diagnosticService;
        this.globalConfDiagnosticConverter = globalConfDiagnosticConverter;
        this.timestampingServiceDiagnosticConverter = timestampingServiceDiagnosticConverter;
        this.ocspResponderDiagnosticConverter = ocspResponderDiagnosticConverter;
    }

    @Override
    @PreAuthorize("hasAuthority('DIAGNOSTICS')")
    public ResponseEntity<GlobalConfDiagnostics> getGlobalConfDiagnostics() {
        DiagnosticsStatus status = diagnosticService.queryGlobalConfStatus();
        return new ResponseEntity<>(globalConfDiagnosticConverter.convert(status), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('DIAGNOSTICS')")
    public ResponseEntity<List<TimestampingServiceDiagnostics>> getTimestampingServicesDiagnostics() {
        List<DiagnosticsStatus> statuses = diagnosticService.queryTimestampingStatus();
        return new ResponseEntity<>(timestampingServiceDiagnosticConverter.convert(statuses), HttpStatus.OK);
    }

    @Override
    @PreAuthorize("hasAuthority('DIAGNOSTICS')")
    public ResponseEntity<List<OcspResponderDiagnostics>> getOcspRespondersDiagnostics() {
        List<OcspResponderDiagnosticsStatus> statuses = diagnosticService.queryOcspResponderStatus();
        return new ResponseEntity<>(ocspResponderDiagnosticConverter.convert(statuses), HttpStatus.OK);
    }
}
