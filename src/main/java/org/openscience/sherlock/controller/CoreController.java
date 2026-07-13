/*
 * MIT License
 *
 * Copyright (c) 2020 Michael Wenk (https://github.com/michaelwenk)
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

package org.openscience.sherlock.controller;

import org.openscience.sherlock.model.QueryTypes;
import org.openscience.sherlock.model.exchange.RequestData;
import org.openscience.sherlock.model.exchange.RequestResult;
import org.openscience.sherlock.utils.Utilities;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Core Controller", description = "Core functionalities of the Sherlock backend services.")
@RestController
@RequestMapping(value = "/")
public class CoreController {

        @Value("${sherlock.version}")
        private String sherlockVersion;

        private final DereplicationController dereplicationController;
        private final ElucidationController elucidationController;
        private final DetectionController detectionController;
        private final RetrievalController retrievalController;
        private final JobController jobController;

        public CoreController(final DereplicationController dereplicationController,
                        final ElucidationController elucidationController,
                        final DetectionController detectionController,
                        final RetrievalController retrievalController, final JobController jobController) {
                this.dereplicationController = dereplicationController;
                this.elucidationController = elucidationController;
                this.detectionController = detectionController;
                this.retrievalController = retrievalController;
                this.jobController = jobController;
        }

        @GetMapping(value = "/", produces = "application/json")
        public ResponseEntity<String> root() {

                return new ResponseEntity<>("Welcome to the Sherlock backend services!"
                                + "\n\n"
                                + "Version: "
                                + sherlockVersion
                                + "\n"
                                + "GitHub: "
                                + "https://github.com/michaelwenk/sherlock"
                                + "\n", HttpStatus.OK);
        }

        @PostMapping(value = "/query", consumes = "application/json", produces = "application/json")
        public ResponseEntity<RequestResult> query(@RequestBody final RequestData requestData) {

                // System.out.println("Received request with query type: "
                // + requestData.getQueryType()
                // + " and data: \n"
                // + requestData.toString()
                // + "\n");

                try {
                        switch (requestData.getQueryType().toUpperCase()) {
                                case QueryTypes.DEREPLICATION:
                                        return this.dereplicationController.dereplicate(requestData);
                                case QueryTypes.ELUCIDATION:
                                        return this.elucidationController.elucidate(requestData);
                                case QueryTypes.ELUCIDATION_ASYNC:
                                        return this.elucidationController.elucidateAsync(requestData);
                                case QueryTypes.DETECTION:
                                        return this.detectionController.detect(requestData);
                                case QueryTypes.RETRIEVE:
                                        return this.retrievalController.getByRequestId(requestData);
                                case QueryTypes.STATUS:
                                        return this.jobController.getJobSnapshot(requestData);
                                case QueryTypes.CANCEL:
                                        return this.jobController.cancelJob(requestData);
                                default:
                                        final RequestResult requestResult = Utilities
                                                        .prepareDefaultRequestResult(requestData);
                                        requestResult.setErrorMessage(
                                                        "Invalid query type: " + requestData.getQueryType());
                                        return new ResponseEntity<>(requestResult, HttpStatus.BAD_REQUEST);
                        }
                } catch (final Exception e) {
                        System.err.println("An error occurred: ");
                        e.printStackTrace();

                        final RequestResult requestResult = Utilities.prepareDefaultRequestResult(requestData);
                        requestResult.setErrorMessage(e.getMessage());
                        return new ResponseEntity<>(requestResult, HttpStatus.INTERNAL_SERVER_ERROR);
                }
        }

}
