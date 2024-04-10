from datetime import timezone, datetime
import logging
import cv2
import numpy as np
from django.contrib.auth import authenticate
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from .models import (
    RegisteredStudent,
    StudentAccount,
    TeacherAccount,
    StudentAttendance,
    Lecture,
)
from .serializers import (
    TeacherSerializer,
    StudentVerifySerializer,
    StudentAccountSerializer,
    RegisteredStudentSerializer,
    LectureSerializer,
    StudentAttendanceSerializer,
)

from scipy.spatial import distance
from django.core.files.base import ContentFile
from django.core.files.storage import default_storage
from django.utils import timezone
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.core.exceptions import ValidationError
from rest_framework.generics import (
    ListAPIView,
    RetrieveUpdateDestroyAPIView,
    CreateAPIView,
)
import face_recognition
from rest_framework.views import APIView
from face_recognition_app import serializers
from rest_framework.generics import RetrieveAPIView
from rest_framework.filters import SearchFilter

@api_view(['POST'])
def mark_attendance(request):
    try:
        unknown_face_image = request.FILES.get('face_data')
        teacher_username = request.data.get('teacher_username', '')
        subject_name = request.data.get('subject', '')

        if not unknown_face_image or not teacher_username or not subject_name:
            return Response({'error': 'Face data, teacher username, or subject not provided'}, status=400)

        temp_image_path = default_storage.save('temp_unknown_face_image.jpeg', ContentFile(unknown_face_image.read()))

        unknown_face_img = face_recognition.load_image_file(temp_image_path)
        face_locations = face_recognition.face_locations(unknown_face_img)

        if not face_locations:
            return Response({'error': 'No face found in the provided image'}, status=400)

        unknown_face_encoding = face_recognition.face_encodings(unknown_face_img, known_face_locations=face_locations)[0]

        recognized_student = None
        max_similarity = 0  # Initialize with a minimum value

        teacher_account = get_object_or_404(TeacherAccount, user__username=teacher_username)
        lecture = get_object_or_404(Lecture, teacher=teacher_account, subject=subject_name)
        today = timezone.now().date()

        for student in RegisteredStudent.objects.all():
            student_photo = face_recognition.load_image_file(student.student_photo.path)
            student_face_locations = face_recognition.face_locations(student_photo)

            if not student_face_locations:
                continue

            student_face_encoding = face_recognition.face_encodings(student_photo, known_face_locations=student_face_locations)[0]

            match_threshold = 0.6  # Adjust this value for better matching sensitivity

            # Compare faces
            match = face_recognition.compare_faces([student_face_encoding], unknown_face_encoding, tolerance=match_threshold)
            
            if match[0]:
                face_distance = face_recognition.face_distance([student_face_encoding], unknown_face_encoding)[0]
                similarity = 1 - face_distance  # Similarity is inversely proportional to face distance

                if similarity > max_similarity:
                    max_similarity = similarity
                    recognized_student = student

        if recognized_student:
            if StudentAttendance.objects.filter(student=recognized_student, datetime__date=today, lecture=lecture).exists():
                default_storage.delete(temp_image_path)
                return Response({'error': f'Attendance already marked for {recognized_student.full_name} today with the same teacher and subject'}, status=400)
            
            update_or_create_attendance_objects(recognized_student, teacher_account, lecture)
            default_storage.delete(temp_image_path)
            return Response({'recognized_student': recognized_student.id})
        else:
            default_storage.delete(temp_image_path)
            return Response({'error': 'No matching face found in student photos'}, status=400)

    except Exception as e:
        default_storage.delete(temp_image_path)
        return Response({'error': 'An error occurred'}, status=500)



def update_or_create_attendance_objects(student_account, teacher_account, lecture):
    current_datetime = timezone.now()

    attendance_object, created = StudentAttendance.objects.get_or_create(
        student=student_account,
        teacher=teacher_account,
        lecture=lecture,
        datetime=current_datetime,
        defaults={'is_present': True}
    )

    if not created:
        attendance_object.is_present = True
        attendance_object.save()


@api_view(['POST'])
def signup_teacher(request):
    serializer = TeacherSerializer(data=request.data)

    if serializer.is_valid():
        serializer.save()
        return Response({"message": "Teacher registered successfully"}, status=status.HTTP_201_CREATED)

    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@api_view(['POST'])
def verify_student(request):
    serializer = StudentVerifySerializer(data=request.data)

    if serializer.is_valid():
        # Student data is valid, indicating a match in the RegisteredStudent model
        return Response({"message": "Student found in Registered Students"}, status=status.HTTP_200_OK)

    # If the data provided does not match any student in the RegisteredStudent model
    return Response(serializer.errors, status=status.HTTP_404_NOT_FOUND)

from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from .serializers import StudentAccountSerializer

@api_view(['POST'])
def create_student_credentials(request):
    serializer = StudentAccountSerializer(data=request.data)

    try:
        serializer.is_valid(raise_exception=True)  # Raise an exception for validation errors

        # If validation passes, save the serializer data
        result = serializer.save()
        return Response(result, status=status.HTTP_201_CREATED)

    except serializers.ValidationError as validation_error:
        # Extract the error message from the validation error
        error_message = str(validation_error.detail[0]) if validation_error.detail else 'Invalid data.'

        return Response({'error': error_message}, status=status.HTTP_400_BAD_REQUEST)


@api_view(['POST'])
def login(request):
    username = request.data.get('username')
    password = request.data.get('password')

    user = authenticate(username=username, password=password)

    if user:
        if hasattr(user, 'teacher_profile'):
            return Response({"message": "Login successful as a teacher", "user_type": "teacher"}, status=status.HTTP_200_OK)
        elif hasattr(user, 'student_user'):
            return Response({"message": "Login successful as a student", "user_type": "student"}, status=status.HTTP_200_OK)

    return Response({"message": "User type not recognized"}, status=status.HTTP_401_UNAUTHORIZED)


class RegisteredStudentListCreateView(APIView):
    def get(self, request):
        teacher_username = request.query_params.get('teacher', None)
        if teacher_username:
            students = RegisteredStudent.objects.filter(teacher__user__username=teacher_username)
        else:
            students = RegisteredStudent.objects.all()

        serializer = RegisteredStudentSerializer(students, many=True)
        return Response(serializer.data)

    def post(self, request):
        serializer = RegisteredStudentSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

   
    def put(self, request, pk):
        student = get_object_or_404(RegisteredStudent, pk=pk)
        serializer = RegisteredStudentSerializer(student, data=request.data)
        
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data)
        
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    def delete(self, request, pk):
        student = RegisteredStudent.objects.get(pk=pk)
        student.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
  



class LectureListCreateView(APIView):
    serializer_class = LectureSerializer

    def get(self, request):
        teacher_username = self.request.query_params.get('teacher_username')
        lectures = Lecture.objects.filter(teacher__user__username=teacher_username)
        serializer = self.serializer_class(lectures, many=True)
        return Response(serializer.data)


    def post(self, request):
        serializer = self.serializer_class(data=request.data, context={'request': request})
        try:
            serializer.is_valid(raise_exception=True)
            serializer.save()
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        except ValidationError as e:
            # Log detailed information about the validation error
            print(f"Validation Error: {e}")
            return Response({"error": "Invalid data. Check the input and try again."}, status=status.HTTP_400_BAD_REQUEST)
        except Exception as e:
            # Log detailed information about the unexpected error
            print(f"Unexpected Error: {e}")
            return Response({"error": "An unexpected error occurred. Please try again later."}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
    def delete(self, request, pk):
        try:
            lecture = Lecture.objects.get(pk=pk)
            lecture.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except Lecture.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)



class GetAllAttendanceView(ListAPIView):
    serializer_class = StudentAttendanceSerializer

    def get_queryset(self):
        teacher_username = self.request.query_params.get('teacher', None)
        if teacher_username:
            return StudentAttendance.objects.filter(teacher__user__username=teacher_username)
        else:
            return StudentAttendance.objects.all()


    def delete(self, request, pk):
        try:
            attendance = StudentAttendance.objects.get(pk=pk)
            attendance.delete()
            return Response(status=status.HTTP_204_NO_CONTENT)
        except Lecture.DoesNotExist:
            return Response(status=status.HTTP_404_NOT_FOUND)



class SearchAttendanceView(ListAPIView):
    serializer_class = StudentAttendanceSerializer

    def get_queryset(self):
        teacher_username = self.request.query_params.get('teacher', None)
        query = self.request.query_params.get('query', None)

        if teacher_username and query:
            # Search for student attendance based on the query and teacher
            return StudentAttendance.objects.filter(
                teacher__user__username=teacher_username,
                student__full_name__icontains=query
            )
        else:
            return StudentAttendance.objects.none()
        


class SearchStudentView(ListAPIView):
    serializer_class = RegisteredStudentSerializer

    def get_queryset(self):
        teacher_username = self.request.query_params.get('teacher', None)
        query = self.request.query_params.get('query', None)

        if teacher_username and query:
            # Search for student attendance based on the query and teacher
            return RegisteredStudent.objects.filter(
                teacher__user__username=teacher_username,
                full_name__icontains=query
            )
        else:
            return RegisteredStudent.objects.none()
        

class GetStudentAttendanceView(ListAPIView):
    serializer_class = StudentAttendanceSerializer

    def get_queryset(self):
        student_username = self.request.query_params.get('student', None)

        if student_username:
            try:
                # Get the StudentAccount based on the student username
                student_account = StudentAccount.objects.get(user__username=student_username)

                # Get the RegisteredStudent associated with the StudentAccount
                registered_student = student_account.student

                # Filter StudentAttendance based on the RegisteredStudent
                queryset = StudentAttendance.objects.filter(student=registered_student)
                return queryset
            except StudentAccount.DoesNotExist:
                return StudentAttendance.objects.none()
        else:
            print("Error in getting the student's attendances")

class SearchStudentAttendanceView(ListAPIView):
    serializer_class = StudentAttendanceSerializer

    def get_queryset(self):
        student_username = self.request.query_params.get('student', None)
        query = self.request.query_params.get('query', None)

        if student_username and query:
            # Get the StudentAccount based on the student username
            student_account = StudentAccount.objects.get(user__username=student_username)

            # Get the RegisteredStudent associated with the StudentAccount
            registered_student = student_account.student

            # Search for student attendance based on the query and student
            queryset = StudentAttendance.objects.filter(
                student=registered_student,
                lecture__subject__icontains=query
            )

            return queryset
        else:
            return StudentAttendance.objects.none()







import csv
from django.http import HttpResponse
from rest_framework.views import APIView
from rest_framework.response import Response
from .models import RegisteredStudent, StudentAttendance, Lecture
from .serializers import RegisteredStudentSerializer
import logging

logger = logging.getLogger(__name__)


from django.http import HttpResponse
from rest_framework.views import APIView
import csv
from .models import RegisteredStudent, StudentAttendance

class ExportStudentAttendanceCSV(APIView):
    def get(self, request, *args, **kwargs):
        teacher_username = request.query_params.get('teacher', None)

        try:
            if teacher_username:
                students = RegisteredStudent.objects.filter(
                    teacher__user__username=teacher_username,
                )

                # Create the CSV response
                response = HttpResponse(content_type='text/csv')
                response['Content-Disposition'] = f'attachment; filename="{teacher_username}_student_attendance.csv"'

                # Create a CSV writer
                writer = csv.writer(response)

                # Write the header
                writer.writerow(['Roll Number', 'Full Name', 'Year', 'Department', 'Lecture Names'])

                # Write the data rows
                for student in students:
                    # Get the attendance data for the student
                    student_attendance = StudentAttendance.objects.filter(student=student)

                    # Get all attended lectures for the student
                    attended_lectures = [attendance.lecture.subject for attendance in student_attendance]

                    # Write the data row
                    writer.writerow([
                        student.roll_number,
                        student.full_name,
                        student.year,
                        student.department,
                        ', '.join(attended_lectures),  # Join all attended lectures with comma
                    ])

                return response

            return Response({'detail': 'Invalid parameters for exporting student attendance.'}, status=400)
        except Exception as e:
            logger.error(f"An error occurred while exporting student attendance: {str(e)}")
            return Response({'detail': 'An error occurred while exporting student attendance.'}, status=500)





































